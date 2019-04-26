package ru.tinkoff.fintech.stocks

import cats.data.{Kleisli, ReaderT}

import scala.concurrent.Future

package object result {

  type Result[T] = ReaderT[Future, Env, T]
  //FutureEither for error handling

  object Result {

    def apply[T](run: Env => Future[T]): Result[T] = Kleisli[Future, Env, T](run)

  }

}
