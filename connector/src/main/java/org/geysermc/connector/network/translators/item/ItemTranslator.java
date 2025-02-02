/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.network.translators.item;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.opennbt.tag.builtin.ByteArrayTag;
import com.github.steveice10.opennbt.tag.builtin.ByteTag;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.DoubleTag;
import com.github.steveice10.opennbt.tag.builtin.FloatTag;
import com.github.steveice10.opennbt.tag.builtin.IntArrayTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.LongArrayTag;
import com.github.steveice10.opennbt.tag.builtin.LongTag;
import com.github.steveice10.opennbt.tag.builtin.ShortTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.protocol.bedrock.data.ItemData;
import org.geysermc.connector.console.GeyserLogger;
import org.geysermc.connector.utils.Remapper;
import org.geysermc.connector.utils.MessageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemTranslator {

    public ItemStack translateToJava(ItemData data) {
        JavaItem javaItem = getJavaItem(data);

        if (data.getTag() == null) {
            return new ItemStack(javaItem.getId(), data.getCount());
        }
        return new ItemStack(javaItem.getId(), data.getCount(), translateToJavaNBT(data.getTag()));
    }

    public ItemData translateToBedrock(ItemStack stack) {
        // Most likely dirt if null
        if (stack == null) {
            return ItemData.of(3, (short)0, 0);
        }

        BedrockItem bedrockItem = getBedrockItem(stack);
        if (stack.getNBT() == null) {
           return ItemData.of(bedrockItem.getId(), (short) bedrockItem.getData(), stack.getAmount());
        }
        return ItemData.of(bedrockItem.getId(), (short) bedrockItem.getData(), stack.getAmount(), translateToBedrockNBT(stack.getNBT()));
    }

    public BedrockItem getBedrockItem(ItemStack stack) {
        BedrockItem bedrockItem = Remapper.ITEM_REMAPPER.convertToBedrock(stack);
        if (bedrockItem == null) {
            GeyserLogger.DEFAULT.debug("Missing mapping for java item " + stack.getId());
            return BedrockItem.AIR;
        }

        return bedrockItem;
    }

    public JavaItem getJavaItem(ItemData data) {
        JavaItem javaItem = Remapper.ITEM_REMAPPER.convertToJava(data);
        if (javaItem == null) {
            GeyserLogger.DEFAULT.debug("Missing mapping for bedrock item " + data.getId() + ":" + data.getDamage());
            return JavaItem.AIR;
        }

        return javaItem;
    }

    private CompoundTag translateToJavaNBT(com.nukkitx.nbt.tag.CompoundTag tag) {
        CompoundTag javaTag = new CompoundTag(tag.getName());
        Map<String, Tag> javaValue = javaTag.getValue();
        if (tag.getValue() != null && !tag.getValue().isEmpty()) {
            for (String str : tag.getValue().keySet()) {
                com.nukkitx.nbt.tag.Tag bedrockTag = tag.get(str);
                Tag translatedTag = translateToJavaNBT(bedrockTag);
                if (translatedTag == null)
                    continue;

                javaValue.put(str, translatedTag);
            }
        }

        return javaTag;
    }

    private Tag translateToJavaNBT(com.nukkitx.nbt.tag.Tag tag) {
        if (tag instanceof com.nukkitx.nbt.tag.ByteArrayTag) {
            com.nukkitx.nbt.tag.ByteArrayTag byteArrayTag = (com.nukkitx.nbt.tag.ByteArrayTag) tag;
            return new ByteArrayTag(byteArrayTag.getName(), byteArrayTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.ByteTag) {
            com.nukkitx.nbt.tag.ByteTag byteTag = (com.nukkitx.nbt.tag.ByteTag) tag;
            return new ByteTag(byteTag.getName(), byteTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.DoubleTag) {
            com.nukkitx.nbt.tag.DoubleTag doubleTag = (com.nukkitx.nbt.tag.DoubleTag) tag;
            return new DoubleTag(doubleTag.getName(), doubleTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.FloatTag) {
            com.nukkitx.nbt.tag.FloatTag floatTag = (com.nukkitx.nbt.tag.FloatTag) tag;
            return new FloatTag(floatTag.getName(), floatTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.IntArrayTag) {
            com.nukkitx.nbt.tag.IntArrayTag intArrayTag = (com.nukkitx.nbt.tag.IntArrayTag) tag;
            return new IntArrayTag(intArrayTag.getName(), intArrayTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.IntTag) {
            com.nukkitx.nbt.tag.IntTag intTag = (com.nukkitx.nbt.tag.IntTag) tag;
            return new IntTag(intTag.getName(), intTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.LongArrayTag) {
            com.nukkitx.nbt.tag.LongArrayTag longArrayTag = (com.nukkitx.nbt.tag.LongArrayTag) tag;
            return new LongArrayTag(longArrayTag.getName(), longArrayTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.LongTag) {
            com.nukkitx.nbt.tag.LongTag longTag = (com.nukkitx.nbt.tag.LongTag) tag;
            return new LongTag(longTag.getName(), longTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.ShortTag) {
            com.nukkitx.nbt.tag.ShortTag shortTag = (com.nukkitx.nbt.tag.ShortTag) tag;
            return new ShortTag(shortTag.getName(), shortTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.StringTag) {
            com.nukkitx.nbt.tag.StringTag stringTag = (com.nukkitx.nbt.tag.StringTag) tag;
            return new StringTag(stringTag.getName(), stringTag.getValue());
        }

        if (tag instanceof com.nukkitx.nbt.tag.ListTag) {
            com.nukkitx.nbt.tag.ListTag listTag = (com.nukkitx.nbt.tag.ListTag) tag;

            List<Tag> tags = new ArrayList<Tag>();
            for (Object value : listTag.getValue()) {
                if (!(value instanceof com.nukkitx.nbt.tag.Tag))
                    continue;

                com.nukkitx.nbt.tag.Tag tagValue = (com.nukkitx.nbt.tag.Tag) value;
                Tag javaTag = translateToJavaNBT(tagValue);
                if (javaTag != null)
                    tags.add(javaTag);
            }
            return new ListTag(listTag.getName(), tags);
        }

        if (tag instanceof com.nukkitx.nbt.tag.CompoundTag) {
            return translateToJavaNBT((com.nukkitx.nbt.tag.CompoundTag) tag);
        }

        return null;
    }

    private com.nukkitx.nbt.tag.CompoundTag translateToBedrockNBT(CompoundTag tag) {
        Map<String, com.nukkitx.nbt.tag.Tag<?>> javaValue = new HashMap<String, com.nukkitx.nbt.tag.Tag<?>>();
        if (tag.getValue() != null && !tag.getValue().isEmpty()) {
            for (String str : tag.getValue().keySet()) {
                Tag javaTag = tag.get(str);
                com.nukkitx.nbt.tag.Tag translatedTag = translateToBedrockNBT(javaTag);
                if (translatedTag == null)
                    continue;

                javaValue.put(str, translatedTag);
            }
        }

        com.nukkitx.nbt.tag.CompoundTag bedrockTag = new com.nukkitx.nbt.tag.CompoundTag(tag.getName(), javaValue);
        return bedrockTag;
    }

    private com.nukkitx.nbt.tag.Tag translateToBedrockNBT(Tag tag) {
        if (tag instanceof ByteArrayTag) {
            ByteArrayTag byteArrayTag = (ByteArrayTag) tag;
            return new com.nukkitx.nbt.tag.ByteArrayTag(byteArrayTag.getName(), byteArrayTag.getValue());
        }

        if (tag instanceof ByteTag) {
            ByteTag byteTag = (ByteTag) tag;
            return new com.nukkitx.nbt.tag.ByteTag(byteTag.getName(), byteTag.getValue());
        }

        if (tag instanceof DoubleTag) {
            DoubleTag doubleTag = (DoubleTag) tag;
            return new com.nukkitx.nbt.tag.DoubleTag(doubleTag.getName(), doubleTag.getValue());
        }

        if (tag instanceof FloatTag) {
            FloatTag floatTag = (FloatTag) tag;
            return new com.nukkitx.nbt.tag.FloatTag(floatTag.getName(), floatTag.getValue());
        }

        if (tag instanceof IntArrayTag) {
            IntArrayTag intArrayTag = (IntArrayTag) tag;
            return new com.nukkitx.nbt.tag.IntArrayTag(intArrayTag.getName(), intArrayTag.getValue());
        }

        if (tag instanceof IntTag) {
            IntTag intTag = (IntTag) tag;
            return new com.nukkitx.nbt.tag.IntTag(intTag.getName(), intTag.getValue());
        }

        if (tag instanceof LongArrayTag) {
            LongArrayTag longArrayTag = (LongArrayTag) tag;
            return new com.nukkitx.nbt.tag.LongArrayTag(longArrayTag.getName(), longArrayTag.getValue());
        }

        if (tag instanceof LongTag) {
            LongTag longTag = (LongTag) tag;
            return new com.nukkitx.nbt.tag.LongTag(longTag.getName(), longTag.getValue());
        }

        if (tag instanceof ShortTag) {
            ShortTag shortTag = (ShortTag) tag;
            return new com.nukkitx.nbt.tag.ShortTag(shortTag.getName(), shortTag.getValue());
        }

        if (tag instanceof StringTag) {
            StringTag stringTag = (StringTag) tag;
            return new com.nukkitx.nbt.tag.StringTag(stringTag.getName(), MessageUtils.getBedrockMessage(Message.fromString(stringTag.getValue())));
        }

        if (tag instanceof ListTag) {
            ListTag listTag = (ListTag) tag;
            if (listTag.getName().equalsIgnoreCase("Lore")) {
                List<com.nukkitx.nbt.tag.StringTag> tags = new ArrayList<>();
                for (Object value : listTag.getValue()) {
                    if (!(value instanceof Tag))
                        continue;

                    com.nukkitx.nbt.tag.StringTag bedrockTag = (com.nukkitx.nbt.tag.StringTag) translateToBedrockNBT((Tag) value);
                    if (bedrockTag != null)
                        tags.add(bedrockTag);
                }
                return new com.nukkitx.nbt.tag.ListTag<>(listTag.getName(), com.nukkitx.nbt.tag.StringTag.class, tags);
            }
        }

        if (tag instanceof CompoundTag) {
            return translateToBedrockNBT((CompoundTag) tag);
        }

        return null;
    }
}
