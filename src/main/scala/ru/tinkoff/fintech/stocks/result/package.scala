package ru.tinkoff.fintech.stocks

import cats.data.{Kleisli, Reader, ReaderT}

import scala.concurrent.Future

package object result {

  type Result[T] = ReaderT[Future, Env, T]

  object Result {

    def apply[T](run: Env => Future[T]): Result[T] = Kleisli[Future, Env, T](run)

    def lift[T](reader: Reader[Env, Future[T]]) = Result(reader.run)
  }

}
