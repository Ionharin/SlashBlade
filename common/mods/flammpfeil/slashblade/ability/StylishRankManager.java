package mods.flammpfeil.slashblade.ability;

import com.google.common.collect.Maps;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.flammpfeil.slashblade.TagPropertyAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.ChatMessageComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.EventPriority;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Map;

/**
 * Created by Furia on 14/07/29.*/

public class StylishRankManager {

    /**
     * 0 < Relative
     * 0 > Absolute
     */
    public static class AttackTypes{
        public static Map<String,Float> types = Maps.newHashMap();
        public static String None = registerAttackType("None", 0);

        public static String Saya1 = registerAttackType("Saya1", 0.3f);
        public static String Saya2 = registerAttackType("Saya2", 0.3f);
        public static String Battou = registerAttackType("Battou", 0.5f);
        public static String IaiBattou = registerAttackType("IaiBattou", -0.5f);

        public static String Kiriage = registerAttackType("Kiriage", 0.3f);
        public static String Kiriorosi = registerAttackType("Kiriorosi", 0.4f);

        public static String Iai = registerAttackType("Iai", 0.3f);
        public static String JumpBattou = registerAttackType("JumpBattou", 0.4f);

        public static String SlashDim = registerAttackType("SlashDim", 0.6f);
        public static String SlashDimMagic = registerAttackType("SlashDimMagic", -0.1f);
        public static String Drive = registerAttackType("Drive", 0.5f);
        public static String Spear = registerAttackType("Spear", -0.2f);
        //public static String WaveEdge = registerAttackType("WaveEdge", -0.1f);

        public static String QuickDrive = registerAttackType("QuickDrive", 0.2f);

        public static String PhantomSword = registerAttackType("PhantomSword", 0.2f);
        public static String BreakPhantomSword = registerAttackType("BreakPhantomSword", 0.1f);

        public static String JustGuard = registerAttackType("JustGuard", 1.0f);
        public static String Noutou = registerAttackType("Noutou", -1.5f);

        public static String DestructObject = registerAttackType("DestructObject", -0.1f);

        public static String registerAttackType(String key,float factor){
            types.put(key,factor);
            return key;
        }
    }

    /**
     * 0:
     * 1:D
     * 2:C
     * 3:B
     * 4:A
     * 5:S
     * 6:SS
     * 7:SSS
     */
    public static String rankText[] = {"D","C","B","A","S","SS","SSS"};

    public static int RankRange = 100;

    public static TagPropertyAccessor.TagPropertyIntegerWithRange RankPoint = new TagPropertyAccessor.TagPropertyIntegerWithRange("SBRankPoint",0, (rankText.length * RankRange));
    public static TagPropertyAccessor.TagPropertyLong LastRankPointUpdate = new TagPropertyAccessor.TagPropertyLong("SBLastRPUpdate");
    public static TagPropertyAccessor.TagPropertyString AttackType = new TagPropertyAccessor.TagPropertyString("LastAttackType");


    public static final int initCooltime = 20;
    public static final int addCooltime = 10;
    public static final int maxCooltime = initCooltime + addCooltime;

    public static NBTTagCompound dummyTag = new NBTTagCompound();
    public static NBTTagCompound getTag(Entity e){
        if(e != null)
            return e.getEntityData();
        else
            return dummyTag;
    }

    public static int getTotalRankPoint(Entity e){
        NBTTagCompound tag = getTag(e);
        int rank = RankPoint.get(tag);

        long now = e.worldObj.getTotalWorldTime();
        long lastUpdate = LastRankPointUpdate.get(tag);

        long descPoint = Math.max(0, now - lastUpdate);

        if(descPoint < RankRange * 2)
            rank -= descPoint;
        else
            rank = 0;

        rank = Math.max(0,rank);

        return rank;
    }

    public static float getCurrentProgress(Entity e){

        int rank = getTotalRankPoint(e);

        rank %= RankRange;

        return rank / (float) RankRange;
    }

    public static int getStylishRank(Entity e){
        return getStylishRank(getTotalRankPoint(e));
    }
    public static int getStylishRank(int totalRankPoint){
        int rank = totalRankPoint;

        rank = (int)Math.ceil(rank / (float)RankRange);

        rank = Math.max(0,Math.min(rankText.length, rank));

        return rank;
    }

    public static String getRankText(Entity e){
        return getRankText(getStylishRank(e));
    }
    public static String getRankText(int rank){
        return rank <= 0 ? "" : rankText[Math.min(rank - 1, rankText.length - 1)];
    }

    public static void setNextAttackType(Entity e,String key){
        NBTTagCompound tag = getTag(e);
        AttackType.set(tag, key);
    }

    public static void doAttack(Entity e){
        if(e == null) return;
        NBTTagCompound tag = getTag(e);

        //stylePoint
        String attackType = AttackType.get(tag);
        addRankPoint(e, attackType);
    }

    public static void addRankPoint(Entity e, String attackType){
        if(e == null) return;
        NBTTagCompound tag = getTag(e);

        //stylePoint
        int value = 0;

        if(AttackTypes.types.containsKey(attackType))
            value =(int)(RankRange * AttackTypes.types.get(attackType));

        if(value == 0)
            return;
        else if(value < 0){
            value = Math.abs(value);

        }else{
            //get last use AttackTypeTime stylePoint calcFactor
            String timerKey = "SBAttackTime" + attackType;
            long last = tag.getLong(timerKey);
            long now = e.worldObj.getTotalWorldTime();

            if(last < now){
                tag.setLong(timerKey,now + initCooltime);
            }else if((last - now) < initCooltime){
                value /= 2;
                tag.setLong(timerKey,Math.min(now + maxCooltime, last + addCooltime));
            }else{
                value = 1;
                tag.setLong(timerKey,now + maxCooltime);
            }

        }

        addRankPoint(e,value);
    }

    public static void addRankPoint(Entity e,int amount){
        if(e == null) return;
        if(e.worldObj.isRemote) return;
        NBTTagCompound tag = getTag(e);

        int rankPoint = getTotalRankPoint(e);

        int lastRank = getStylishRank(rankPoint);

        rankPoint += amount;
        RankPoint.set(tag, rankPoint);
        LastRankPointUpdate.set(tag,e.worldObj.getTotalWorldTime());

        int postRank = getStylishRank(rankPoint);

        onRiseInRank(e, postRank, rankPoint);
    }

    public static final String MessageHeader = "///RankUpdate ";
    public static final String MessageHurt = "///RankUpdateHurt";

    public static void onRiseInRank(Entity e, int rank,int rankPoint){
        if(e == null) return;

        NBTTagCompound tag = getTag(e);

        if(e instanceof EntityPlayer){
            //((EntityPlayer)e).addChatMessage(new ChatComponentText(getRankText(rank) + ":" + rankPoint + ":" + AttackType.get(tag)));
            ((EntityPlayer)e).addChatMessage(MessageHeader + rankPoint);
        }
    }

    public static void onHurtChangeRank(Entity e){
        if(e == null) return;
        if(e instanceof EntityPlayer){
            ((EntityPlayer)e).addChatMessage(MessageHurt);
        }
    }

    @ForgeSubscribe(priority = EventPriority.LOWEST)
    public void LivingHurtEvent(LivingHurtEvent e){
        String type = e.source.getDamageType();
        if(e.isCanceled()) return;
        if(e.entity == null) return;
        if(!(e.entity instanceof EntityPlayer)) return;

        //guard�s�ł��Amob����̍U���ł͂Ȃ�
        if(e.source.isUnblockable() && e.source.getEntity() != null) return;

        NBTTagCompound tag = getTag(e.entity);

        Long lastUpdate = LastRankPointUpdate.get(tag);

        LastRankPointUpdate.set(tag,lastUpdate - RankRange / 2);
        onHurtChangeRank(e.entity);
    }

    @SideOnly(Side.CLIENT)
    @ForgeSubscribe
    public void ClientChatReceivedEvent(ClientChatReceivedEvent e){
        String text = ChatMessageComponent.createFromJson(e.message).toStringWithFormatting(true);
        if(text.startsWith(MessageHeader)){

            String value = text.substring(MessageHeader.length());
            int rankPoint;
            try{
                rankPoint = Integer.parseInt(value);
            }catch(Exception ex){
                rankPoint = 0;
            }

            Entity el = Minecraft.getMinecraft().thePlayer;
            NBTTagCompound tag = getTag(el);
            RankPoint.set(tag,rankPoint);
            LastRankPointUpdate.set(tag,el.worldObj.getTotalWorldTime());

            //Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("receive :" + rankPoint + ":" + e.message.getUnformattedText()));

            e.setCanceled(true);
        }else if(text.startsWith(MessageHurt)){
            Entity el = net.minecraft.client.Minecraft.getMinecraft().thePlayer;
            NBTTagCompound tag = getTag(el);

            Long lastUpdate = LastRankPointUpdate.get(tag);

            LastRankPointUpdate.set(tag, lastUpdate - RankRange / 2);
            e.setCanceled(true);
        }
    }
}
