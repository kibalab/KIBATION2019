package com.comduck.chatbot.discord;

import AudioCore.GuildMusicManager;
import AudioCore.PlayerManager;
import AudioCore.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.client.entities.Application;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.managers.AudioManager;

import java.awt.Color;
import java.lang.reflect.Array;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DiscordBotMain extends ListenerAdapter {

    int globalVolume = 10;
    Queue commandQueue = new LinkedList();

    public static void main(String[] args) throws Exception {
        new DiscordBotMain().start();
    }

    private void start() throws Exception {
        JDABuilder builder = new JDABuilder(AccountType.BOT);
        String token = "NjA2NDc1NzE4MzA1Nzc1NjM2.XULmjw.vYwYU3M816BsjuW-mXxXauGVVx4";
        builder.setToken(token);

        builder.addEventListener(this);
        builder.buildAsync();
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        super.onShutdown(event);
    }

    //리액션 수신 이벤트
    @Override
    public void onGenericMessageReaction(GenericMessageReactionEvent event) {
        super.onGenericMessageReaction(event);
        System.out.println(String.format("{'Type': 'Reaction', 'Guild_Name': '%s', 'Chennal_Name': '%s', 'Author': '%s', 'Reaction': '%s', 'Message_ID': '%s'}", event.getGuild().getName(), event.getChannel().getName(), event.getUser().getName(), event.getReaction(), event.getMessageId() ));
        if( !event.getUser().isBot() )
        {
            onReactionBindCommand(event);
        }
    }

    //리액션 커맨드
    private void onReactionBindCommand(GenericMessageReactionEvent event)
    {
        //Stop
        if( event.getReactionEmote().getName().equals("⏹") )
        {
            event.getChannel().sendMessage(String.format(
                    "> 대기열 재생 중지 ``%s``",
                    event.getUser().getName()
            )).queue();
            PlayerManager manager = PlayerManager.getInstance();
            GuildMusicManager musicManager = manager.getGuildMusicManager(event.getGuild());
            AudioPlayer player = musicManager.player;
            TrackScheduler scheduler = musicManager.scheduler;

            scheduler.getQueue().clear();
            player.stopTrack();
            player.setPaused(false);
        }
        if( event.getReactionEmote().getName().equals("⏭") )
        {
            PlayerManager manager = PlayerManager.getInstance();
            GuildMusicManager musicManager = manager.getGuildMusicManager(event.getGuild());
            AudioPlayer player = musicManager.player;
            TrackScheduler scheduler = musicManager.scheduler;

            if(player.getPlayingTrack() == null)
            {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(new Color(0xff6624));
                eb.addField("경고 Warning", String.format(
                        "대기열이 비어 있습니다.\n``%s``",
                        event.getUser().getName()
                ), false);
                event.getChannel().sendMessage(eb.build()).queue();
            }
            else
            {
                event.getChannel().sendMessage(String.format(
                        "> 곡 스킵 ``%s``",
                        event.getUser().getName()
                )).queue();
                scheduler.nextTrack();
            }
        }
        //printURL
        if( event.getReactionEmote().getName().equals("🎦") )
        {
            PlayerManager manager = PlayerManager.getInstance();
            GuildMusicManager musicManager = manager.getGuildMusicManager(event.getGuild());
            AudioPlayer player = musicManager.player;
            TrackScheduler scheduler = musicManager.scheduler;

            event.getChannel().sendMessage(String.format("> %s", player.getPlayingTrack().getInfo().uri)).queue();
        }
        if( event.getReactionEmote().getName().equals("\uD83C\uDFB6") )
        {
            PlayerManager manager = PlayerManager.getInstance();
            GuildMusicManager musicManager = manager.getGuildMusicManager(event.getGuild());
            AudioPlayer player = musicManager.player;
            TrackScheduler scheduler = musicManager.scheduler;

            if(player.getPlayingTrack() == null){
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(new Color(0xff6624));
                eb.addField("경고 Warning", String.format(
                        "대기열이 비어 있습니다.\n``%s``",
                        event.getUser().getName().toString()
                ), false);
                event.getChannel().sendMessage(eb.build()).queue();
            }
            else
            {
                AudioTrackInfo info = player.getPlayingTrack().getInfo();

                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(new Color(0x244aff));
                eb.setTitle(String.format(
                        "재생중 - %s [%s/%s]",
                        info.title,
                        formatTime(player.getPlayingTrack().getPosition()),
                        formatTime(player.getPlayingTrack().getDuration())
                ));
                List playelist = new ArrayList(scheduler.getQueue());
                String str = "";
                if(playelist.size() != 0) {
                    for (int i = 0;  true; i++) {
                        if ( playelist.size() == i )
                        {
                            break;
                        }
                        AudioTrack t = (AudioTrack) playelist.get(i);
                        str += String.format("%d. %s\n", i+1, t.getInfo().title);
                    }
                }
                else{str = "None";}
                eb.addField("TrackList", str, false);
                event.getChannel().sendMessage(eb.build()).queue();
            }
        }
        if( event.getReactionEmote().getName().equals("\uD83D\uDD00") ){
            PlayerManager manager = PlayerManager.getInstance();
            GuildMusicManager musicManager = manager.getGuildMusicManager(event.getGuild());
            AudioPlayer player = musicManager.player;
            TrackScheduler scheduler = musicManager.scheduler;
            Queue queue = scheduler.getQueue();
            List<AudioTrack> list = new ArrayList<>();

            if(queue.size() != 0){
                for(int i=0; true; i++){
                    list.add((AudioTrack) queue.poll());
                    if( queue.size() == 0 ){
                        break;
                    }
                }

                Collections.shuffle(list);

                for(int i=0; true; i++){
                    queue.offer(list.get(i));
                    if( queue.size() == list.size() ){
                        break;
                    }
                }
                event.getChannel().sendMessage(String.format("> 대기열 셔플 ``%s``", event.getUser().getName())).queue();
            }
            else{
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(new Color(0xff6624));
                eb.addField("경고 Warning", String.format(
                        "대기열이 비어 있습니다.\n``%s``",
                        event.getUser().getName().toString()
                ), false);
                event.getChannel().sendMessage(eb.build()).queue();
            }
        }
    }

    //메시지 수신 이벤트
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        //로그 출력
        System.out.println(String.format("{'Type': 'Message', 'Guild_Name': '%s', 'Chennal_Name': '%s', 'Author': '%s', 'Context': '%s'}", event.getGuild().getName(), event.getChannel().getName(), event.getAuthor().getName(), event.getMessage().getContentRaw() ));
        //System.out.println("[Log] " + event.getGuild().getName() + event.getAuthor().getName().getName() + " : " + event.getMessage().getContentDisplay());

        if( event.getAuthor().isBot() ){
            if( commandQueue.size() >= 1 )
            {
                ReactionInterface(event);
            }
        }

        //커맨드 모음에 데이터 인풋
        boolean commandRun =  CommandInterface(event);
        if(!commandRun){
            //System.out.println( String.format( "{'Error': 'Unknown Command', 'Context': '%s'}", event.getMessage().getContentRaw() ) );
        }
    }

    private  boolean ReactionInterface(MessageReceivedEvent event) {
        Message sendMsg = event.getMessage();
        if( commandQueue.poll() == "play" )
        {
            wait_reaction(sendMsg, "⏹" );//stop
            wait_reaction(sendMsg, "⏭" );//skip
            wait_reaction(sendMsg, "\uD83C\uDFA6" );//printURL
            wait_reaction(sendMsg, "\uD83C\uDFB6" );//tracklist
            wait_reaction(sendMsg, "\uD83D\uDD00" );//Shuffle
            return true;
        }
        return false;
    }

    //입력 데이터 처리
    private boolean CommandInterface(MessageReceivedEvent event) {
        //접두사 여부 식별
        String msg = "";
        if( !event.getMessage().getContentRaw().startsWith("?") )
        {return false;}
        else
        {
            msg = event.getMessage().getContentRaw();
            msg = msg.substring(1, msg.length());
        }
        if( event.getGuild().getName() == "Nerine force" )
        {
            if( event.getChannel().getName() != "bot-command" ){return false;}
        }
        BotCommands(event, msg);
        //명령어가 없을경우 false반환
        return false;
    }

    //명렁어 실행
    private void BotCommands(final MessageReceivedEvent event, String msg)
    {
        if( msg.startsWith("test") )
        {
            cmd_test(event);
        }
        else if( msg.startsWith("play") )
        {
            cmd_play(event, msg);
        }
        else if( msg.startsWith("join") )
        {
            cmd_join(event);
        }
        else if( msg.startsWith("leave") || msg.startsWith("out"))
        {
            cmd_leave(event);
        }
        else if( msg.startsWith("stop") )
        {
            cmd_stop(event);
        }
        else if( msg.startsWith("skip") || msg.startsWith("next"))
        {
            cmd_skip(event);
        }
        else if( msg.startsWith("volume") || msg.startsWith("vol") )
        {
            cmd_volume(event, msg);
        }
        else if( msg.startsWith("tracklist") || msg.startsWith("songlist") || msg.startsWith("tlist") || msg.startsWith("slist"))
        {
            cmd_tracklist(event);
        }
        else if( msg.startsWith("goto") )
        {
            cmd_goto(event, msg);
        }
        else if( msg.startsWith("shuffle") ||  msg.startsWith("mix") || msg.startsWith("sf"))
        {
            cmd_shuffle(event, msg);
        }
        else if( msg.startsWith("repeat") || msg.startsWith("replay") || msg.startsWith("rp") )
        {
            cmd_repeat(event);
        }
    }

    //명령어 함수////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private boolean cmd_test(MessageReceivedEvent event)
    {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("OK", null);
        eb.setColor(new Color(0x244aff));
        eb.addField("Test Embed", "이 Embed 메시지는 다용도 테스트 메시지 입니다.", false);
        event.getChannel().sendMessage(eb.build()).queue();
        Message sendMsg = event.getMessage();
        wait_reaction(sendMsg, "⏹" );
        return true;
    }
    private boolean cmd_play(MessageReceivedEvent event, String msg)
    {
        String url = msg.replaceFirst("play ", "");

        VoiceChannel Vch = event.getMember().getVoiceState().getChannel();
        AudioManager audiomng = event.getGuild().getAudioManager();
        audiomng.openAudioConnection(Vch);

        PlayerManager manager = PlayerManager.getInstance();
        manager.loadAndPlay(event, url);
        manager.getGuildMusicManager(event.getGuild()).player.setVolume(globalVolume);

        commandQueue.add("play");
        return true;
    }
    private boolean cmd_join(MessageReceivedEvent event)
    {
        VoiceChannel Vch = event.getMember().getVoiceState().getChannel();
        if( event.getGuild().getName() == "Nerine force" )
        {
            if( Vch.getName() != "Music" ){return false;}
        }
        event.getChannel().sendMessage(String.format(
                "> %s 입장 ``%s``",
                Vch.getName(),
                event.getAuthor().getName()
        )).queue();
        AudioManager audiomng = event.getGuild().getAudioManager();
        audiomng.openAudioConnection(Vch);
        return true;
    }
    private boolean cmd_leave(MessageReceivedEvent event)
    {
        VoiceChannel Vch = event.getGuild().getSelfMember().getVoiceState().getChannel();
        event.getChannel().sendMessage(String.format(
                "> %s 퇴장 ``%s``",
                Vch.getName(),
                event.getAuthor().getName()
        )).queue();
        event.getGuild().getAudioManager().closeAudioConnection();

        PlayerManager manager = PlayerManager.getInstance();
        GuildMusicManager musicManager = manager.getGuildMusicManager(event.getGuild());
        AudioPlayer player = musicManager.player;
        TrackScheduler scheduler = musicManager.scheduler;

        scheduler.getQueue().clear();
        player.stopTrack();
        player.setPaused(false);
        return true;
    }
    private boolean cmd_stop(MessageReceivedEvent event)
    {
        event.getChannel().sendMessage(String.format(
                "> 대기열 재생 중지 ``%s``",
                event.getAuthor().getName()
        )).queue();
        PlayerManager manager = PlayerManager.getInstance();
        GuildMusicManager musicManager = manager.getGuildMusicManager(event.getGuild());
        AudioPlayer player = musicManager.player;
        TrackScheduler scheduler = musicManager.scheduler;



        scheduler.getQueue().clear();
        player.stopTrack();
        player.setPaused(false);
        return true;
    }
    private boolean cmd_skip(MessageReceivedEvent event)
    {
        PlayerManager manager = PlayerManager.getInstance();
        GuildMusicManager musicManager = manager.getGuildMusicManager(event.getGuild());
        AudioPlayer player = musicManager.player;
        TrackScheduler scheduler = musicManager.scheduler;

        if(player.getPlayingTrack() == null)
        {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(new Color(0xff6624));
            eb.addField("경고 Warning", String.format(
                    "대기열이 비어 있습니다.\n``%s``",
                    event.getAuthor().getName().toString()
            ), false);
            event.getChannel().sendMessage(eb.build()).queue();
        }
        else
        {
            event.getChannel().sendMessage(String.format(
                    "> 곡 스킵 ``%s``",
                    event.getAuthor().getName()
            )).queue();
            scheduler.nextTrack();
        }
        return true;
    }
    private boolean cmd_volume(MessageReceivedEvent event, String msg)
    {
        String _Nvol = msg.replaceFirst("volume ", "");
        PlayerManager manager = PlayerManager.getInstance();
        GuildMusicManager musicManager = manager.getGuildMusicManager(event.getGuild());
        AudioPlayer player = musicManager.player;

        int Ovol = player.getVolume();
        int Nvol = Integer.parseInt(_Nvol);

        if(Nvol > 100){
            Nvol = 100;
        }

        event.getChannel().sendMessage(String.format(
                "> 음량 제어 %d->%s",
                Ovol,
                Nvol
        )).queue();
        globalVolume = Nvol;
        player.setVolume(globalVolume);
        return true;
    }
    private boolean cmd_tracklist(MessageReceivedEvent event)
    {
        PlayerManager manager = PlayerManager.getInstance();
        GuildMusicManager musicManager = manager.getGuildMusicManager(event.getGuild());
        AudioPlayer player = musicManager.player;
        TrackScheduler scheduler = musicManager.scheduler;

        if(player.getPlayingTrack() == null){
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(new Color(0xff6624));
            eb.addField("경고 Warning", String.format(
                    "대기열이 비어 있습니다.\n``%s``",
                    event.getAuthor().getName().toString()
            ), false);
            event.getChannel().sendMessage(eb.build()).queue();
        }
        else
        {
            AudioTrackInfo info = player.getPlayingTrack().getInfo();

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(new Color(0x244aff));
            eb.setTitle(String.format(
                    "재생중 - %s [%s/%s]",
                    info.title,
                    formatTime(player.getPlayingTrack().getPosition()),
                    formatTime(player.getPlayingTrack().getDuration())
            ));
            List playelist = new ArrayList(scheduler.getQueue());
            String str = "";
            if(playelist.size() != 0) {
                for (int i = 0;  true; i++) {
                    if ( playelist.size() == i )
                    {
                        break;
                    }
                    AudioTrack t = (AudioTrack) playelist.get(i);
                    str += String.format("%d. %s\n", i+1, t.getInfo().title);
                }
            }
            else{str = "None";}
            eb.addField("TrackList", str, false);
            event.getChannel().sendMessage(eb.build()).queue();
        }
        return true;
    }
    private boolean cmd_goto(MessageReceivedEvent event, String msg)
    {
        msg = msg.replaceFirst("goto ", "");
        PlayerManager manager = PlayerManager.getInstance();
        GuildMusicManager musicManager = manager.getGuildMusicManager(event.getGuild());
        AudioPlayer player = musicManager.player;
        TrackScheduler scheduler = musicManager.scheduler;

        if(player.getPlayingTrack() == null)
        {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(new Color(0xff6624));
            eb.addField("경고 Warning", String.format(
                    "대기열이 비어 있습니다.\n``%s``",
                    event.getAuthor().getName().toString()
            ), false);
            event.getChannel().sendMessage(eb.build()).queue();
            return false;
        }
        else
        {
            long time = formatLong(msg);
            System.out.println(time);
            player.getPlayingTrack().setPosition(time);

        }
        return true;
    }
    private boolean cmd_shuffle(MessageReceivedEvent event, String msg)
    {
        PlayerManager manager = PlayerManager.getInstance();
        GuildMusicManager musicManager = manager.getGuildMusicManager(event.getGuild());
        AudioPlayer player = musicManager.player;
        TrackScheduler scheduler = musicManager.scheduler;
        Queue queue = scheduler.getQueue();
        List<AudioTrack> list = new ArrayList<>();

        for(int i=0; true; i++){
            list.add((AudioTrack) queue.poll());
            if( queue.size() == 0 ){
                break;
            }
        }

        Collections.shuffle(list);

        for(int i=0; true; i++){
            queue.offer(list.get(i));
            if( queue.size() == list.size() ){
                break;
            }
        }
        event.getChannel().sendMessage(String.format("> 대기열 셔플 ``%s``", event.getAuthor().getName())).queue();
        return true;
    }
    private boolean cmd_repeat(MessageReceivedEvent event)
    {
        PlayerManager manager = PlayerManager.getInstance();
        GuildMusicManager musicManager = manager.getGuildMusicManager(event.getGuild());
        AudioPlayer player = musicManager.player;
        TrackScheduler scheduler = musicManager.scheduler;
        String msg = "play " + player.getPlayingTrack().getInfo().uri;
        cmd_play(event, msg);
        event.getChannel().sendMessage(String.format("> 현재곡 재등록 ``%s``", event.getAuthor().getName())).queue();
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private String formatTime(long time)
    {
        final long h = time / TimeUnit.HOURS.toMillis(1);
        final long m = time % TimeUnit.HOURS.toMillis(1) / TimeUnit.MINUTES.toMillis(1);
        final long s = time % TimeUnit.MINUTES.toMillis(1) / TimeUnit.SECONDS.toMillis(1);

        if(h != 0){
            return String.format("%2d:%2d:%2d", h, m, s);
        }
        else
        {
            return String.format("%2d:%2d", m, s);
        }
    }

    private long formatLong(String msg) {
        String[] StrTime = msg.split(":");
        long LongTime = 0;
        if( StrTime.length == 3 ){
            LongTime += Long.parseLong(StrTime[0]) * 60 * 60;
            LongTime += Long.parseLong(StrTime[1]) * 60;
            LongTime += Long.parseLong(StrTime[2]);
        }
        else if( StrTime.length == 2 ){
            LongTime += Long.parseLong(StrTime[0]) * 60;
            LongTime += Long.parseLong(StrTime[1]);
        }
        else
        {
            LongTime += Long.parseLong(StrTime[1]);
        }

        return LongTime*1000;
    }

    //미완성
    private void wait_reaction(Message msg, String emote)
    {
        msg.addReaction(emote).queue();
    }
}
