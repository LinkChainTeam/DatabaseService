package co.linkchain.services.database.http;

import co.linkchain.services.database.DatabaseService;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

@RestController
public class DatabaseListener {

    @RequestMapping(value = "/db/createpackage", method = RequestMethod.GET)
    public boolean create(@RequestParam("id") long id) {
        DatabaseService.getDatabase().query("INSERT INTO `packages`(packageid, lastupdate) VALUES(" + id + ", -1);");
        return true;
    }

    @RequestMapping(value = "/db/setlatestupdate", method = RequestMethod.GET)
    public boolean setUpdate(@RequestParam("id") long id, @RequestParam("latest") long last) {
        DatabaseService.getDatabase().query("UPDATE `packages` SET `lastupdate`=" + last + " WHERE `packageid`=" + id + ";");
        return true;
    }

    @RequestMapping(value = "/db/getlatestforid", method = RequestMethod.GET)
    public Long getLatest(@RequestParam("id") long id) throws SQLException {
        return DatabaseService.getDatabase().query("SELECT * FROM `packages` WHERE `packageid`=" + id + ";")
                .getResults()
                .stream()
                .findFirst().get()
                .getLong("lastupdate");
    }

    @RequestMapping(value = "/db/temp/add", method = RequestMethod.POST)
    public Long addToFakeChain(@RequestBody String data) throws SQLException {
        DatabaseService.getDatabase().query("INSERT INTO `fakechain` (value) VALUES (\"" + data + "\");").getResults();
        return DatabaseService.getDatabase().query("SELECT * FROM `fakechain` WHERE `id`=(SELECT MAX(id) FROM `fakechain` WHERE `value`=\"" + data + "\");")
                .getResults()
                .stream()
                .findFirst().get()
                .getLong("id");
    }

    @RequestMapping(value = "/db/temp/get", method = RequestMethod.GET)
    public String getFromFakeChain(@RequestParam("id") long id) throws SQLException {
        return DatabaseService.getDatabase().query("SELECT * FROM `fakechain` WHERE `id`=" + id + ";")
                .getResults()
                .stream()
                .findFirst().get()
                .getString("value");
    }
}
