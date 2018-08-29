package co.linkchain.services.database.db;


import co.linkchain.services.common.util.Tuple;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class DatabaseConnectionPool{
    private static class Lock{}
    private final Lock lock = new Lock();

    private List<DBPair> threads;
    private Queue<DatabaseRequest> requests;

    private String address;
    private String username;
    private String pw;
    private int amount;

    public DatabaseConnectionPool(String address, String username, String pw, int amount){
        this.address = address;
        this.username = username;
        this.pw = pw;
        this.amount = amount;
        requests = new LinkedBlockingQueue<>(500);
    }

    public void run(){
        var threadlist = new ArrayList<DBPair>();

        for(int i = 0; i < amount; i++){
            var dbconnthread = new DatabaseConnectionThread(address, decrypt(username, pw));
            var thread = new Thread(dbconnthread);

            thread.setDaemon(true);
            thread.setName("Database Connection " + i);

            var pair = new DBPair(dbconnthread, thread);

            threadlist.add(pair);
        }

        threads = List.copyOf(threadlist);

        threads.forEach(p -> p.thread.start());
    }

    private DBCredentials decrypt(String username, String password){
        return new DBCredentials(username, password);
    }

    public SQLFuture<SQLResult> query(String command){
        DatabaseRequest request = new DatabaseRequest();
        request.value = command;
        request.future = new SQLFuture<SQLResult>();

        requests.add(request);
        synchronized(lock){
            lock.notifyAll();
        }

        return request.future;
    }

    public SQLFuture<SQLProcedureResult> execPrepared(String statement, String[] args){
        DatabaseRequest request = new DatabaseRequest();
        request.value = statement;
        request.future = new SQLFuture<SQLProcedureResult>();

        request.args = args;
        request.prepared = true;

        requests.add(request);
        synchronized(lock){
            lock.notifyAll();
        }

        return request.future;
    }

    private class DBPair{
        DatabaseConnectionThread connection;
        Thread thread;

        public DBPair(DatabaseConnectionThread connection, Thread thread){
            this.connection = connection;
            this.thread = thread;
        }

        public DatabaseConnectionThread getConnection(){
            return connection;
        }

        public Thread getThread(){
            return thread;
        }
    }

    private class DatabaseRequest{
        String value;
        SQLFuture future;
        String[] args;
        List<Tuple<Database.ValueTypes, Object>> vals;
        boolean prepared = false;
    }

    private class DatabaseConnectionThread implements Runnable{
        private Map<String, CallableStatement> statements = new HashMap<>();
        private volatile boolean run = true;
        private String address;
        private DBCredentials creds;

        public DatabaseConnectionThread(String address, DBCredentials creds){
            this.address = address;
            this.creds = creds;
        }

        @Override
        public void run()  {
            Properties properties = new Properties();
            properties.put("user", creds.getUsername());
            properties.put("password", creds.getSinglePassword());
            //properties.put("useSSL", "false");
            try(Connection connection =
                        DriverManager.getConnection(address, properties)){
                while(run){
                    DatabaseRequest request = getNextValue();
                    if(request == null) continue;


                    if(request.prepared){
                        CallableStatement statement = statements.getOrDefault
                                (request.value,
                                connection.prepareCall(request.value));

                        statements.putIfAbsent(request.value, statement);

                        for(int i = 0; i < request.vals.size(); i++){
                            var argument = request.vals.get(i);

                            switch(argument.getU()){
                                case INT:
                                    statement.setInt(i, (Integer) argument.getV());
                                case FLOAT:
                                    statement.setFloat(i, (Float) argument.getV());
                                case DOUBLE:
                                    statement.setDouble(i, (Double) argument.getV());
                                case BYTE:
                                    statement.setByte(i, (Byte) argument.getV());
                                case BOOLEAN:
                                    statement.setInt(i, (Integer) argument.getV());
                                case STRING:
                                    statement.setInt(i, (Integer) argument.getV());

                            }
                        }
                    }else{
                        var statement = connection.createStatement();
                        try{
                            statement.execute(request.value);
                        }catch (SQLException e){
                            throw new RuntimeException("Error on statement " + request.value, e);
                        }
                        request.future.set(new SQLResult(statement.getResultSet()), statement.getUpdateCount());
                    }

                }
            }catch(SQLException e){
                e.printStackTrace();
            }
        }
    }

    private DatabaseRequest getNextValue(){
        synchronized(lock){
            while(requests.isEmpty()){
                try{
                    lock.wait();
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
        return requests.poll();
    }
}


