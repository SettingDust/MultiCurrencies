package me.settingdust.multicurrencies;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.text.serializer.TextSerializers;

/**
 * @author The EpicBanItem Team
 */
@SuppressWarnings("WeakerAccess")
public class TextUtil {

    /**
     * @param origin origin string support FormatText
     * @param keySet placeholders in the string
     * @return TextTemplate
     */
    public static TextTemplate parseTextTemplate(String origin, Set<String> keySet) {
        if (keySet.isEmpty()) {
            return TextTemplate.of(parseFormatText(origin));
        }
        List<Object> objects = new ArrayList<>();
        String[] subStrings = origin.split("\\{");
        for (int i = 0; i < subStrings.length; i++) {
            String subString = subStrings[i];
            if (subString.isEmpty()) {
                continue;
            }
            if (i == 0) {
                objects.add(parseFormatText(subString));
                continue;
            }
            String[] muSub = subString.split("}");
            if (muSub.length == 1 && subString.endsWith("}") && keySet.contains(muSub[0])) {
                objects.add(TextTemplate.arg(muSub[0]));
            } else if (muSub.length > 1 && keySet.contains(muSub[0])) {
                objects.add(TextTemplate.arg(muSub[0]));
                StringBuilder left = new StringBuilder(muSub[1]);
                for (int j = 2; j < muSub.length; j++) {
                    left.append("}");
                    left.append(muSub[j]);
                }
                if (subString.endsWith("}")) {
                    left.append("}");
                }
                objects.add(parseFormatText(left.toString()));
            } else {
                objects.add(parseFormatText("{" + subString));
            }
        }
        Text text = null;
        for (Object o: objects) {
            if (o instanceof Text) {
                text = getLastElement((Text) o);
            } else if (o instanceof TextTemplate.Arg.Builder) {
                if (text != null) {
                    ((TextTemplate.Arg.Builder) o).format(text.getFormat());
                }
            }
        }
        return TextTemplate.of(objects.toArray());
    }

    private static Text getLastElement(Text text) {
        List<Text> children = text.getChildren();
        return children.isEmpty() ? text : children.get(children.size() - 1);
    }


    public static Text parseFormatText(String in) {
        return TextSerializers.FORMATTING_CODE.deserializeUnchecked(in);
    }
}
