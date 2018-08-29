package co.linkchain.services.database.db.impl;

import co.linkchain.services.database.db.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProductionDatabase implements Database {
    Logger logger = LoggerFactory.getLogger(ProductionDatabase.class);
    public static final int AMOUNT = 10;
    private String name, user, pw;
    private DatabaseConnectionPool pool;

    public ProductionDatabase(String name, String user, String pw){
        this.name = name;
        this.user = user;
        this.pw = pw;
        logger.info("Connecting to fakechain " + name.substring(name.lastIndexOf("/") + 1) +  " at " + name.substring(0, name.lastIndexOf("/")) + " with username " + user);
        pool = new DatabaseConnectionPool(name, user, pw, AMOUNT);
        pool.run();
    }

    @Override
    public SQLResult requestQuery(String query){
        return pool.query(query).getResults();
    }

    @Override
    public SQLFuture query(String query){
        return pool.query(query);
    }

    @Override
    public SQLProcedureResult runProcedure(String procname, String... args) {
        return pool.execPrepared(procname, args).getResults();
    }


    @Override
    public void disconnect(){
        //todo
    }
}
