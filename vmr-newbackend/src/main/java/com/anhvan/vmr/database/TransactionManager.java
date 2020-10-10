package com.anhvan.vmr.database;

import com.anhvan.vmr.entity.FutureStateHolder;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class TransactionManager {
  private FutureStateHolder stateHolder;
  private MySQLPool pool;
  private Transaction transaction;
  private SqlConnection connection;

  public TransactionManager(MySQLPool pool) {
    this.pool = pool;
    stateHolder = new FutureStateHolder();
  }

  public TransactionManager(MySQLPool pool, FutureStateHolder holder) {
    this.pool = pool;
    this.stateHolder = holder;
  }

  public <T> T get(String key) {
    return stateHolder.get(key);
  }

  public void set(String key, Object value) {
    stateHolder.set(key, value);
  }

  public Future<TransactionManager> begin() {
    Promise<TransactionManager> promise = Promise.promise();

    pool.getConnection(
        connAr -> {
          if (connAr.failed()) {
            promise.fail(connAr.cause());
            return;
          }

          connection = connAr.result();
          transaction = connection.begin();
          promise.complete(this);
        });

    return promise.future();
  }

  public Future<TransactionManager> commit() {
    Promise<TransactionManager> promise = Promise.promise();

    transaction.commit(
        ar -> {
          if (ar.failed()) {
            log.error("Error when commit transaction: ", ar.cause());
            promise.fail(ar.cause());
          }
          connection.close();
        });

    return promise.future();
  }

  public Future<TransactionManager> rollback() {
    Promise<TransactionManager> promise = Promise.promise();

    transaction.rollback(
        ar -> {
          if (ar.failed()) {
            log.error("Error when rollback transaction: ", ar.cause());
            promise.fail(ar.cause());
          }
          connection.close();
        });

    return promise.future();
  }

  public Transaction getTransaction() {
    return transaction;
  }
}
