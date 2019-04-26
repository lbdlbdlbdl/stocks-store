package ru.tinkoff.fintech.stocks

import io.getquill.{Escape, PostgresAsyncContext}

package object dao {

  val quillContext: PostgresAsyncContext[Escape] =
    new PostgresAsyncContext(Escape, "ru.tinkoff.fintech.stocks.db")

}
