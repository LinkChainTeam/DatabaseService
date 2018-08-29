package co.linkchain.services.database.db;

import co.linkchain.services.database.db.impl.ProductionDatabase;

public interface Database{
    static Database getDatabase(String url, String name, String password){
        return new ProductionDatabase(url, name, password);
    }

    SQLResult requestQuery(String query);

    SQLFuture<SQLResult> query(String query);

    SQLProcedureResult runProcedure(String procname, String... args);

    void disconnect();

    enum ValueTypes{
        STRING, INT, FLOAT, DOUBLE, BYTE, BOOLEAN;
    }
}
