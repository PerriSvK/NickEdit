package sk.perri.kc.nickedit;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.ChatColor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.*;

public class Database
{
    private Connection conn;
    public static List<String> columns = Arrays.asList("group", "tabPrefix", "tabSuffix", "nickPrefix", "nickSuffix",
            "chatPrefix", "chatSuffix", "chatColor", "chatHover");

    Database()
    {
        try
        {
            conn = null;
            DriverManager.setLoginTimeout(1000);
            conn = DriverManager.getConnection("jdbc:mysql://" + Main.self.getConfig().getString("db.host") +
                    ":3306/" +Main.self.getConfig().getString("db.db") + "?useSSL=no&user="+
                    Main.self.getConfig().getString("db.user")+"&password=" +
                    Main.self.getConfig().getString("db.pass") + "&useUnicode=true&characterEncoding=UTF-8" +
                    "&autoReconnect=true&failOverReadOnly=false&maxReconnects=10&connectTimeout=2000&socketTimeout=2000");
            Main.self.getLogger().info("[I] Pripojene ku databaze "+Main.self.getConfig().getString("db.db"));
            createTable();
        }
        catch (Exception e)
        {
            Main.self.getLogger().warning("[E] Neviem sa pripojit ku databaze: "+e.toString());
        }
    }

    private void createTable()
    {
        try
        {
            StringBuilder sb = new StringBuilder();
            columns.forEach(c -> sb.append(", `").append(c).append("` VARCHAR(50) NOT NULL"));
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS ne_groups(" +
                            "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY"+sb.toString()+")");
        }
        catch (Exception e)
        {
            Main.self.getLogger().warning("[E] Neviem vytvorit tabulku "+e.toString());
        }
    }

    public Map<String, Map<String, String>> loadFormat()
    {
        Map<String, Map<String, String>> res = new HashMap<>();

        try
        {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM ne_groups");
            while(rs.next())
            {
                Map<String, String> row = new HashMap<>();
                for(String c : columns)
                    row.put(c, ChatColor.translateAlternateColorCodes('&', rs.getString(c)));

                row.put("id", Integer.toString(rs.getInt("id")));
                res.put(row.get("group"), row);
            }
            Main.self.getLogger().info("[I] Nacitanych "+res.size()+" skupin");
        }
        catch(Exception e)
        {
            Main.self.getLogger().warning("[E] Neviem nacitat data "+e.toString());
        }

        return res;
    }
}
