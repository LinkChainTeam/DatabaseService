package co.linkchain.services.database;

import co.linkchain.services.database.db.Database;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DatabaseService {
    private static Database database;

    public static void main(String[] args)
    {
        database = Database.getDatabase("jdbc:mysql://localhost:3306" + "/" +"linkchain",
                "root", "maligna101");
        SpringApplication.run(DatabaseService.class, args);
    }

    public static Database getDatabase(){
        return database;
    }
}
