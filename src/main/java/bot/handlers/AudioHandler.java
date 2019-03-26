package bot.handlers;

import audio.GuildMusicManager;
import audio.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.AudioManager;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AudioHandler {

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    public AudioHandler(){
        this.musicManagers = new HashMap<>();

        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    public synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    public void connectToVoiceChannel(AudioManager audioManager) {
        if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            VoiceChannel voiceChannel = audioManager.getGuild().getVoiceChannels().get(0);
            audioManager.openAudioConnection(voiceChannel);
        }
    }

    public void connectToVoiceChannel(User user, Guild guild){
        List<VoiceChannel> voiceChannels = guild.getVoiceChannels();
        AudioManager audioManager = guild.getAudioManager();

        for(VoiceChannel voiceChannel : voiceChannels){
            for(Member member : voiceChannel.getMembers()){
                if(user.getId().equals(member.getUser().getId())){
                    audioManager.openAudioConnection(voiceChannel);
                    return;
                }
            }
        }
        connectToVoiceChannel(audioManager);
    }

    public void connectToVoiceChannel(String channelName, Guild guild){
        List<VoiceChannel> voiceChannels = guild.getVoiceChannels();
        AudioManager audioManager = guild.getAudioManager();

        for(VoiceChannel voiceChannel : voiceChannels){
            if(voiceChannel.getName().equals(channelName)){
                audioManager.openAudioConnection(voiceChannel);
            }
        }
    }

    public void loadAndPlay(final TextChannel channel, final String trackUrl) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        Guild guild = channel.getGuild();

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                channel.sendMessage("Adding to queue " + track.getInfo().title).queue();

                connectToVoiceChannel(guild.getAudioManager());
                musicManager.scheduler.queue(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {

                TrackScheduler scheduler = musicManager.scheduler;

                channel.sendMessage("adding items from " + playlist.getName() + " to queue").queue();
                connectToVoiceChannel(guild.getAudioManager());
                for(AudioTrack track : playlist.getTracks()){
                    scheduler.queue(track);
                }
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Nothing found by " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("Could not play: " + exception.getMessage()).queue();
            }
        });
    }

    public boolean pause(Guild guild){
        AudioPlayer audioPlayer = getGuildAudioPlayer(guild).player;
        if(audioPlayer.isPaused()){
            guild.getController().setNickname(guild.getSelfMember(), "Bottinator").queue();
            audioPlayer.setPaused(false);
            return false;
        }
        else {
            guild.getController().setNickname(guild.getSelfMember(), "Bottinator (PAUSED)").queue();
            audioPlayer.setPaused(true);
            return true;
        }
    }

    public void skipTrack(Guild guild) {
        GuildMusicManager musicManager = getGuildAudioPlayer(guild);
        musicManager.scheduler.nextTrack();
    }

    public void leaveVoiceChannel(Guild guild){

        AudioManager audioManager = guild.getAudioManager();
        if(audioManager.isConnected()){
            audioManager.closeAudioConnection();
            GuildMusicManager musicManager = getGuildAudioPlayer(guild);

            if(!musicManager.player.isPaused() && musicManager.player.getPlayingTrack() != null)
                pause(guild);
        }
    }

    public String queuedTracks(Guild guild){
        GuildMusicManager musicManager = getGuildAudioPlayer(guild);

        StringBuilder msgBuilder = new StringBuilder();
        msgBuilder.append("- ");
        musicManager.scheduler.queuedTracks().stream()
                .forEach(title -> { msgBuilder.append(title); msgBuilder.append("\n- "); });

        if(msgBuilder.length() > 0)
            msgBuilder.deleteCharAt(msgBuilder.lastIndexOf("\n- "));

        return msgBuilder.toString();
    }

    public String currentTrack(GuildMusicManager musicManager) {
        if(musicManager.player.getPlayingTrack() != null)
            return musicManager.player.getPlayingTrack().getInfo().title;
        else
            return "";
    }

    public int getVolume(Guild guild) {
        return getGuildAudioPlayer(guild).player.getVolume();
    }

    public void setVolume(Guild guild, int volume) {
        getGuildAudioPlayer(guild).player.setVolume(volume);
    }
}
