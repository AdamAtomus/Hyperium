/*
 *     Copyright (C) 2018  Hyperium <https://hyperium.cc/>
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cc.hyperium.handlers.handlers;

import cc.hyperium.Hyperium;
import cc.hyperium.handlers.handlers.chat.GeneralChatHandler;
import cc.hyperium.mods.sk1ercommon.Multithreading;
import cc.hyperium.mods.sk1ercommon.Sk1erMod;
import cc.hyperium.utils.JsonHolder;
import club.sk1er.website.api.requests.HypixelApiGuild;
import club.sk1er.website.api.requests.HypixelApiPlayer;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ApiDataHandler {

    //Only User's data for now. Will branch out to other things soon

    private final Map<String, HypixelApiPlayer> otherPlayers = new ConcurrentHashMap<>();
    private JsonHolder friends = new JsonHolder();
    private HypixelApiPlayer player = new HypixelApiPlayer(new JsonHolder());
    private List<UUID> friendUUIDList = new ArrayList<>();
    private ConcurrentHashMap<String, HypixelApiGuild> guilds_player = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, HypixelApiGuild> guilds_name = new ConcurrentHashMap<>();

    public ApiDataHandler() {

    }

    public void post() {
        Multithreading.schedule(() -> {
            try {
                if (Hyperium.INSTANCE.getHandlers().getHypixelDetector().isHypixel())
                    refreshFriends();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 10L, 305, TimeUnit.SECONDS);
    }

    public HypixelApiPlayer getPlayer(String name) {
        return otherPlayers.computeIfAbsent(name.toLowerCase(), s -> {
            Multithreading.runAsync(() -> {
                String raw = Sk1erMod.getInstance().
                        rawWithAgent("https://api.sk1er.club/player/" + s);
                HypixelApiPlayer player = new HypixelApiPlayer(new JsonHolder(raw));
                player.getRoot().put("localCache", System.currentTimeMillis());
                if (otherPlayers.size() > 1000)
                    otherPlayers.clear();
                otherPlayers.put(name.toLowerCase(), player);

            });
            return new HypixelApiPlayer(new JsonHolder().put("unloaded", true));
        });

    }

    public List<UUID> getFriendUUIDList() {
        return friendUUIDList;
    }

    public void refreshFriends() {
        if (friends.optBoolean("fetching"))
            return;
        friends.put("fetching", true);
        Multithreading.runAsync(() -> {
            try {
                friends = new JsonHolder(Sk1erMod.getInstance().
                        rawWithAgent("https://api.sk1er.club/friends/" +
                                Minecraft.getMinecraft().getSession().getPlayerID()))
                        .put("localCache", System.currentTimeMillis());
                friendUUIDList = new ArrayList<>();
                for (String s : friends.getKeys()) {
                    try {
                        friendUUIDList.add(cc.hyperium.netty.utils.Utils.dashMeUp(s));
                    } catch (Exception e) {

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                GeneralChatHandler.instance().sendMessage("Something went wrong while loading your friends");
            }
        });
    }

    public JsonHolder getFriends() {
        //5 minute cache
        if (System.currentTimeMillis() - friends.optLong("localCache") > 1000 * 60 * 5)
            refreshFriends();
        return friends;
    }

    private void refreshPlayer() {
        if (player.getRoot().optBoolean("fetching"))
            return;
        player.getRoot().put("fetching", true);
        Multithreading.runAsync(() -> {
            try {
                player = new HypixelApiPlayer(new JsonHolder(Sk1erMod.getInstance().
                        rawWithAgent("https://api.sk1er.club/player/" +
                                Minecraft.getMinecraft().getSession().getPlayerID() +
                                "/" + Sk1erMod.getInstance().getApIKey())));
                player.getRoot().put("localCache", System.currentTimeMillis());
            } catch (Exception e) {

                GeneralChatHandler.instance().sendMessage("Something went wrong while loading your data");
            }
        });
    }

    public HypixelApiPlayer getPlayer() {
        if (System.currentTimeMillis() - player.getRoot().optLong("localCache") > 1000 * 60 * 5)
            refreshPlayer();
        return player;
    }

    public HypixelApiGuild getGuildByPlayer(String uuid) {
        return guilds_player.computeIfAbsent(uuid.toLowerCase(), s -> {
            Multithreading.runAsync(() -> {
                String raw = Sk1erMod.getInstance().
                        rawWithAgent("https://api.sk1er.club/guild/player/" + s);
                HypixelApiGuild guild = new HypixelApiGuild(new JsonHolder(raw));
                guild.getRoot().put("localCache", System.currentTimeMillis());
                guilds_player.put(uuid.toLowerCase(), guild);
            });
            return new HypixelApiGuild(new JsonHolder().put("unloaded", true));
        });
    }

    public HypixelApiGuild getGuildByName(String name) {
        return guilds_name.computeIfAbsent(name, s -> {
            Multithreading.runAsync(() -> {
                String raw = Sk1erMod.getInstance().
                        rawWithAgent("https://api.sk1er.club/guild/name/" + s.replace(" ", "+"));
                HypixelApiGuild guild = new HypixelApiGuild(new JsonHolder(raw));
                guild.getRoot().put("localCache", System.currentTimeMillis());
                guilds_name.put(name.toLowerCase(), guild);
            });
            return new HypixelApiGuild(new JsonHolder().put("unloaded", true));
        });
    }
}
