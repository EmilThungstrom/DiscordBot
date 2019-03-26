package bot;

import bot.handlers.AudioHandler;
import bot.handlers.SteamStoreHandler;
import net.dv8tion.jda.client.entities.Group;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import random.Random;

import javax.security.auth.login.LoginException;
import java.util.List;

@Component
public class Bottinator extends ListenerAdapter
{

    private final String token;
    private final Random random;

    private final AudioHandler audioHandler;
    private final SteamStoreHandler steamStoreHandler;

    @Autowired
    public Bottinator(@Value("${TOKEN}") String token, Random random, AudioHandler audioHandler, SteamStoreHandler steamStoreHandler){
        this.token = token;
        this.random = random;
        this.audioHandler = audioHandler;
        this.steamStoreHandler = steamStoreHandler;
        try
        {
            JDA jda = new JDABuilder(this.token)         // The token of the account that is logging in.
                    .addEventListener(this)  // An instance of a class that will handle events.
                    .build();
            jda.awaitReady(); // Blocking guarantees that JDA will be completely loaded.
            System.out.println("Finished Building JDA!");
        }
        catch (LoginException e)
        {
            //If anything goes wrong in terms of authentication, this is the exception that will represent it
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            //Due to the fact that awaitReady is a blocking method, one which waits until JDA is fully loaded,
            // the waiting can be interrupted. This is the exception that would fire in that situation.
            //As a note: in this extremely simplified example this will never occur. In fact, this will never occur unless
            // you use awaitReady in a thread that has the possibility of being interrupted (async thread usage and interrupts)
            e.printStackTrace();
        }
    }
    /**
     * NOTE THE @Override!
     * This method is actually overriding a method in the ListenerAdapter class! We place an @Override annotation
     *  right before any method that is overriding another to guarantee to ourselves that it is actually overriding
     *  a method from a super class properly. You should do this every time you override a method!
     *
     * As stated above, this method is overriding a hook method in the
     * {@link net.dv8tion.jda.core.hooks.ListenerAdapter ListenerAdapter} class. It has convience methods for all JDA events!
     * Consider looking through the events it offers if you plan to use the ListenerAdapter.
     *
     * In this example, when a message is received it is printed to the console.
     *
     * @param event
     *          An event containing information about a {@link net.dv8tion.jda.core.entities.Message Message} that was
     *          sent in a channel.
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        //These are provided with every event in JDA
        JDA jda = event.getJDA();                       //JDA, the core of the api.
        long responseNumber = event.getResponseNumber();//The amount of discord events that JDA has received since the last reconnect.

        //Event specific information
        User author = event.getAuthor();                //The user that sent the message
        Message message = event.getMessage();           //The message that was received.
        MessageChannel channel = event.getChannel();    //This is the MessageChannel that the message was sent to.
        //  This could be a TextChannel, PrivateChannel, or Group!

        String msg = message.getContentDisplay();              //This returns a human readable version of the Message. Similar to

        if(author.isBot())
            return;

        if (event.isFromType(ChannelType.TEXT))         //If this message was sent to a Guild TextChannel
        {
            //Because we now know that this message was sent in a Guild, we can do guild specific things
            // Note, if you don't check the ChannelType before using these methods, they might return null due
            // the message possibly not being from a Guild!

            Guild guild = event.getGuild();             //The Guild that this message was sent in. (note, in the API, Guilds are Servers)
            TextChannel textChannel = event.getTextChannel(); //The TextChannel that this message was sent to.
            Member member = event.getMember();          //This Member that sent the message. Contains Guild specific information about the User!

            String name;
            if (message.isWebhookMessage())
            {
                name = author.getName();                //If this is a Webhook message, then there is no Member associated
            }                                           // with the User, thus we default to the author for name.
            else
            {
                name = member.getEffectiveName();       //This will either use the Member's nickname if they have one,
            }                                           // otherwise it will default to their username. (User#getName())

            System.out.printf("(%s)[%s]<%s>: %s\n", guild.getName(), textChannel.getName(), name, msg);
        }
        else if (event.isFromType(ChannelType.PRIVATE)) //If this message was sent to a PrivateChannel
        {
            //The message was sent in a PrivateChannel.
            //In this example we don't directly use the privateChannel, however, be sure, there are uses for it!
            PrivateChannel privateChannel = event.getPrivateChannel();

            System.out.printf("[PRIV]<%s>: %s\n", author.getName(), msg);
        }
        else if (event.isFromType(ChannelType.GROUP))   //If this message was sent to a Group. This is CLIENT only!
        {
            //The message was sent in a Group. It should be noted that Groups are CLIENT only.
            Group group = event.getGroup();
            String groupName = group.getName() != null ? group.getName() : "";  //A group name can be null due to it being unnamed.

            System.out.printf("[GRP: %s]<%s>: %s\n", groupName, author.getName(), msg);
        }

        if(msg.startsWith("!"))
            interpretCommand(event);
    }

    private void interpretCommand(MessageReceivedEvent event){

        String[] command = event.getMessage().getContentRaw().split(" ", 2);

        switch (command[0]) {
            case "!help":
                helpCommand(command, event.getTextChannel());
                break;
            case "!roll":
                rollCommand(event.getChannel(), command);
                break;
            case "!play":
                audioHandler.loadAndPlay(event.getTextChannel(), command[1]);
                break;
            case "!skip":
                audioHandler.skipTrack(event.getGuild());
                break;
            case "!leave":
                audioHandler.leaveVoiceChannel(event.getGuild());
                break;
            case "!connect":
                connectCommand(command, event.getAuthor(), event.getGuild());
                break;
            case "!queue":
                queueCommand(event.getTextChannel(), event.getGuild());
                break;
            case "!pause":
                audioHandler.pause(event.getGuild());
                break;
            case "!track":
                event.getTextChannel().sendMessage(audioHandler.currentTrack(audioHandler.getGuildAudioPlayer(event.getGuild()))).queue();
                break;
            case "!volume":
                volumeCommand(command, event.getGuild(), event.getTextChannel());
                break;
            case "!store":
                storeCommand(command, event.getTextChannel());

        }
    }
    //audio commands
    //-------------------------------------------------------------------------------------------------------------------
    public void connectCommand(String[] command, User user, Guild guild){

        if(command.length == 1)
            audioHandler.connectToVoiceChannel(user, guild);
        else
            audioHandler.connectToVoiceChannel(command[1], guild);
    }

    private void queueCommand(TextChannel textChannel, Guild guild){

        String queuedTracks = audioHandler.queuedTracks(guild);

        if(queuedTracks.isEmpty())
            textChannel.sendMessage("The track queue is empty!").queue();
        else
            textChannel.sendMessage(queuedTracks).queue();
    }

    private void volumeCommand(String[] command, Guild guild, TextChannel channel){

        if(command.length == 2 && command[1].matches("[1-9]?[0-9]")) {
            audioHandler.setVolume(guild, Integer.parseInt(command[1]));
        }
        else {
            channel.sendMessage("Current volume is: " + audioHandler.getVolume(guild)).queue();
        }
    }

    //store commands
    //-------------------------------------------------------------------------------------------------------------------
    private void storeCommand(String[] command, TextChannel channel){
        if(command.length != 2)
            return;

        List<String> appIDs = steamStoreHandler.getAppIDs(command[1].toLowerCase().trim());

        if(appIDs.isEmpty())
            return;

        appIDs.stream().forEach(appID -> {
            String url = "https://store.steampowered.com/app/" + appID;

            if(!steamStoreHandler.redirectsToFrontPage(url).equals("https://store.steampowered.com/"))
                channel.sendMessage(url).queue();
        });
    }

    //roll commands
    //-------------------------------------------------------------------------------------------------------------------
    private void rollCommand(MessageChannel channel, String[] command) {

        if(!command[1].matches("^[1-9][0-9]{0,2}d[1-9][0-9]{0,9}((\\s?[+-]\\s?[1-9][0-9]{0,9})|(#[1-9][0-9]{0,9}))?$")){
            channel.sendMessage("Invalid format, valid format [1-999]d[1-9999999999][+/-][1-9999999999]").queue();
            return;
        }

        String[] params = command[1].replaceAll(" ", "").split("d");

        int diceAmount = Integer.parseInt(params[0]);

        String retMessage;
        if(params[1].contains("#")) {

            String[] sizeAndTarget = params[1].split("#");

            int diceSize = Integer.parseInt(sizeAndTarget[0]);
            int targetNumber = Integer.parseInt(sizeAndTarget[1]);

            retMessage = rollForSuccesses(targetNumber, diceAmount, diceSize);
        }
        else {
            int modifier = 0;
            int diceSize;

            if(params[1].contains("+")){
                String[] sizeAndModifier = params[1].split("\\+");
                diceSize = Integer.parseInt(sizeAndModifier[0]);
                modifier = Integer.parseInt(sizeAndModifier[1]);
            }
            else if(params[1].contains("-")){
                String[] sizeAndModifier = params[1].split("-");
                diceSize = Integer.parseInt(sizeAndModifier[0]);
                modifier = -Integer.parseInt(sizeAndModifier[1]);
            }
            else{
                diceSize = Integer.parseInt(params[1]);
            }

            retMessage = rollForTotal(modifier, diceAmount, diceSize);
        }

        channel.sendMessage(retMessage).queue();
    }

    private String rollForTotal(int modifier, int diceAmount, int diceSize){

        StringBuilder retMessage = new StringBuilder();
        retMessage.append("[ ");

        int randInt;
        int total = modifier;
        for(int i = 0; i < diceAmount; i++) {
            randInt = random.nextInt(diceSize) + 1;
            total += randInt;
            retMessage.append(randInt);
            retMessage.append(", ");
        }
        retMessage.deleteCharAt(retMessage.lastIndexOf(","));
        retMessage.append("]\n");
        retMessage.append("total: " + total);

        return retMessage.toString();
    }

    private String rollForSuccesses(int targetNumber, int diceAmount, int diceSize) {

        StringBuilder retMessage = new StringBuilder();
        retMessage.append("[ ");

        int total = 0;
        int randInt;
        for(int i = 0; i < diceAmount; i++) {
            randInt = random.nextInt(diceSize) + 1;


            if(randInt >= targetNumber){
                total++;
                retMessage.append("**" + randInt + "**");
            }
            else{
                retMessage.append(randInt);
            }

            retMessage.append(", ");
        }
        retMessage.deleteCharAt(retMessage.lastIndexOf(","));
        retMessage.append("]\n");
        retMessage.append("total successes: " + total);

        return retMessage.toString();
    }
    //Help commands
    //-------------------------------------------------------------------------------------------------------------------
    private void helpCommand(String[] command, TextChannel channel){

        channel.sendMessage("!help - bot will write out this list\n" +
                "!roll [number]d[value]+/-[modifier] - bot will roll the given dices for you\n" +
                "!play [url] - bot will add the given track to the queue\n" +
                "!volume [0-99] - will set the bot volume\n" +
                "!pause - bot will pause/unpause itself\n" +
                "!skip - bot will skip to the next track in the queue\n" +
                "!track - bot will write the title of the current track playing\n" +
                "!queue - bot will write out a list of all the queued tracks\n" +
                "!connect [channel name] - bot will either join the channel given, the user's channel or the first channel in the guild\n" +
                "!leave - bot will leave any audio channel and pause any audio playing\n" +
                "!store [app name] - bot will search the steam store for partial name matches and list them\n")
                .queue();
    }
}