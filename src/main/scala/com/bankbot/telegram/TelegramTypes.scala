package com.bankbot.telegram

import spray.json._

/**
  * Case classes used in Telegram Api
  */

object TelegramTypes {

  case class KeyboardButton(text: String, request_contact: Boolean, request_location: Boolean)

  case class ReplyKeyboardMarkup(keyboard: Array[KeyboardButton], resize_keyboard: Boolean, one_time_keyboard: Boolean)

  case class Contact(phone_number: String, first_name: String)

  case class User(id: Int, first_name: String, last_name: String, username: String)

  case class Chat(id: Int, c_type: String)

  case class Message(message_id: Int, from: User, chat: Chat, date: Int, text: Option[String])

  case class Update(update_id: Int, message: Message)

  case class ServerAnswer(ok: Boolean, result: Array[Update])

}

/**
  * Json Marshalling for case classes used in Telegram Api
  */

trait MessageMarshallingTelegram extends DefaultJsonProtocol {

  import TelegramTypes._

  implicit val keyboardButtonFormat = jsonFormat3(KeyboardButton)
  implicit val replyKeyboardMarkupFormat = jsonFormat3(ReplyKeyboardMarkup)
  implicit val contactFormat = jsonFormat2(Contact)
  implicit val userFormat = jsonFormat4(User)
  implicit val chatFormat = jsonFormat(Chat, "id", "type")
  implicit val messageFormat = jsonFormat5(Message)
  implicit val updateFormat = jsonFormat2(Update)
  implicit val serverAnswerFormat = jsonFormat2(ServerAnswer)

}
