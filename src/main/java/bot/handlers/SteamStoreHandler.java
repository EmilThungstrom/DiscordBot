package bot.handlers;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;


@Component
public class SteamStoreHandler {

    private final String url = "http://api.steampowered.com/ISteamApps/GetAppList/v0002/?key=STEAMKEY&format=json";

    private JSONArray apps;

    public SteamStoreHandler(){
        fetchData();
    }

    private void fetchData() {

        BufferedReader in = null;

        try {
            URL urlObj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            int responseCode = connection.getResponseCode();

            in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            //Read JSON response and print
            JSONObject myResponse = new JSONObject(content.toString());
            apps = myResponse.getJSONObject("applist").getJSONArray("apps");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if( in != null) { try { in.close(); } catch (Exception e) {  }}
        }
    }

    /**
     * Performs a HTTP get request for the given url to check if and where it redirects to
     *
     * @param url the url to check for redirect
     * @return is empty unless the url redirects in which case it contains the destination
     *
     * @author Emil Thungstrom
     */
    public String redirectsToFrontPage(String url) {

        String retUrl = "";
        try {
            URL urlObj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();

            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");

            if(connection.getResponseCode() == 302){
                retUrl = connection.getHeaderFields().get("Location").get(0);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return retUrl;
    }

    /**
     * Querries the steam store web api for the appId of a given app name
     *
     * @param getName full or partial name of the steam app
     * @return list contains only the appID and is empty if no matches are found
     *
     * @author Emil Thungstrom
     */
    public List<String> getAppIDs(String getName) {

        List<String> appIDs = new LinkedList<>();
        JSONObject app;
        for(int i = 0; i < apps.length(); i++) {
            app = apps.getJSONObject(i);
            String appName = app.getString("name").toLowerCase();
            if(appName.contains(getName)
                    && !appName.contains("-")
                    && !appName.contains("trailer")) {
                System.out.println(app);
                appIDs.add(Integer.toString(app.getInt("appid")));
            }
        }
        return appIDs;
    }

}
