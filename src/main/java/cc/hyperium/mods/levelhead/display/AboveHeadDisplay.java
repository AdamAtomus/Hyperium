package cc.hyperium.mods.levelhead.display;

import cc.hyperium.mods.levelhead.Levelhead;
import cc.hyperium.utils.ChatColor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.Team;

import java.util.ArrayList;
import java.util.UUID;

public class AboveHeadDisplay extends LevelheadDisplay {

    private boolean bottomValue = true;
    private int index;

    public AboveHeadDisplay(DisplayConfig config) {
        super(DisplayPosition.ABOVE_HEAD, config);
    }

    @Override
    public void tick() {
        for (EntityPlayer player : Minecraft.getMinecraft().theWorld.playerEntities) {
            if (!existedMoreThan5Seconds.contains(player.getUniqueID())) {
                if (!timeCheck.containsKey(player.getUniqueID())) {
                    timeCheck.put(player.getUniqueID(), 0);
                }

                int old = timeCheck.get(player.getUniqueID());
                if (old > 100) {
                    if (!existedMoreThan5Seconds.contains(player.getUniqueID())) {
                        existedMoreThan5Seconds.add(player.getUniqueID());
                    }
                } else if (!player.isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer)) {
                    timeCheck.put(player.getUniqueID(), old + 1);
                }
            }

            if (loadOrRender(player)) {
                UUID uuid = player.getUniqueID();
                if (!cache.containsKey(uuid)) {
                    Levelhead.getInstance().fetch(uuid, this, bottomValue);
                }
            }
        }
    }

    @Override
    public void checkCacheSize() {
        int max = Math.max(150, Levelhead.getInstance().getDisplayManager().getMasterConfig().getPurgeSize());
        if (cache.size() > max) {
            ArrayList<UUID> safePlayers = new ArrayList<>();

            for (EntityPlayer player : Minecraft.getMinecraft().theWorld.playerEntities) {
                if (existedMoreThan5Seconds.contains(player.getUniqueID())) {
                    safePlayers.add(player.getUniqueID());
                }
            }

            existedMoreThan5Seconds.clear();
            existedMoreThan5Seconds.addAll(safePlayers);

            for (UUID uuid : cache.keySet()) {
                if (!safePlayers.contains(uuid)) {
                    cache.remove(uuid);
                    trueValueCache.remove(uuid);
                }
            }
        }
    }

    @Override
    public void onDelete() {
        cache.clear();
        trueValueCache.clear();
        existedMoreThan5Seconds.clear();
    }

    @Override
    public boolean loadOrRender(EntityPlayer player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getPotionID() == 14) {
                return false;
            }
        }

        if (!renderFromTeam(player)) {
            return false;
        }

        if (player.riddenByEntity != null) {
            return false;
        }

        int renderDistance = Levelhead.getInstance().getDisplayManager().getMasterConfig().getRenderDistance();
        int min = Math.min(64 * 64, renderDistance * renderDistance);

        if (player.getDistanceSqToEntity(Minecraft.getMinecraft().thePlayer) > min) {
            return false;
        }

        if (player.hasCustomName() && player.getCustomNameTag().isEmpty()) {
            return false;
        }

        if (player.getDisplayName().toString().isEmpty()) {
            return false;
        }

        if (!existedMoreThan5Seconds.contains(player.getUniqueID())) {
            return false;
        }

        if (player.getDisplayName().getFormattedText().contains(ChatColor.COLOR_CHAR + "k")) {
            return false;
        }

        if (player.isInvisible() || player.isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer)) {
            return false;
        }

        if (player.isSneaking()) {
            return false;
        }

        return true;
    }

    private boolean renderFromTeam(EntityPlayer player) {
        Team team = player.getTeam();
        Team team1 = Minecraft.getMinecraft().thePlayer.getTeam();

        if (team != null) {
            Team.EnumVisible visibility = team.getNameTagVisibility();
            switch (visibility) {
                case NEVER:
                    return false;

                case HIDE_FOR_OTHER_TEAMS:
                    return team1 == null || team.isSameTeam(team1);

                case HIDE_FOR_OWN_TEAM:
                    return team1 == null || !team.isSameTeam(team1);

                case ALWAYS:
                default:
                    return true;
            }
        }

        return true;
    }

    public boolean isBottomValue() {
        return bottomValue;
    }

    public void setBottomValue(boolean bottomValue) {
        this.bottomValue = bottomValue;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}