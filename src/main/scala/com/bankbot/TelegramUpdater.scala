package com.bankbot

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.event.LoggingAdapter
import com.bankbot.telegram.TelegramApi
import com.bankbot.telegram.PrettyMessage.{prettyHelp, prettyNonPrivate}
import telegram.TelegramTypes.{Message, ServerAnswer}

/**
  * Actor that gets Telegram Updates
  */

object TelegramUpdater {
  def props(sessionManager: ActorRef, noSessionActions: ActorRef, telegramApi: TelegramApi) =
    Props(classOf[TelegramUpdater], sessionManager, noSessionActions, telegramApi)
  val name = "telegram-updater"
  case object getOffset
  case class Offset(offset: Int)
  case object StartProcessing
  case object StopProcessing
}

class TelegramUpdater(sessionManager: ActorRef, noSessionActions: ActorRef,
                      telegramApi: TelegramApi)
  extends Actor with ActorLogging {
  import TelegramUpdater._
  implicit val logger: LoggingAdapter = log

  log.info("TelegramUpdater created! " + context.self.path.toString)

  private var offset = 0

  def init: Receive = {
    case StartProcessing => {
      log.info("TelegramUpdater starts processing messages! " + context.self.path.toString)
      context.become(work)
      telegramApi.getUpdates(offset)
    }
  }

  def work: Receive = {
    case StopProcessing => {
      log.info("TelegramUpdater stops processing messages! " + context.self.path.toString)
      context.become(init)
    }
    case ServerAnswer(true, result) => {
      for (update <- result) {
        if (update.message.chat.c_type == "private") {
          update.message match {
            case Message(_, Some(user), chat, _, None, Some(text), None) => {
              text match {
                case s if s == "/rates" || s == "/r" => {
                  noSessionActions ! NoSessionActions.SendRates(chat.id)
                }
                case s if s == "/balance" || s == "/b" => {
                  sessionManager ! UserSession.BalanceCommand(user, chat.id)
                }
                case s if s == "/history" || s == "/hi" => {
                  sessionManager ! UserSession.HistoryCommand(user, chat.id)
                }
                case s if s == "/help" || s == "/h" => {
                  noSessionActions ! NoSessionActions.SendMessage(chat.id, prettyHelp)
                }
                case s if s == "/start" || s == "/s" => {
                  noSessionActions ! NoSessionActions.SendMessage(chat.id, prettyHelp)
                }
                case s => {
                  noSessionActions ! NoSessionActions.SendMessage(chat.id, "No Such Command\nSee /help")
                }
              }
            }
            case Message(_, _, _, _, _, _, Some(contact)) => {
              sessionManager ! SessionManager.PossibleContact(update.message.chat.id, contact)
            }
            case Message(message_id, Some(user), chat, _, Some(reply), Some(text), None) => {
              sessionManager ! SessionManager.PossibleReply(chat.id, reply.message_id, text)
            }
          }
        } else {
          noSessionActions ! NoSessionActions.SendMessage(update.message.chat.id, prettyNonPrivate)
        }
        offset = update.update_id + 1
      }
      telegramApi.getUpdates(offset)
    }
    case ServerAnswer(false, _) => telegramApi.getUpdates(offset)
    case getOffset => sender ! Offset(offset)
  }

  def receive = init

}
