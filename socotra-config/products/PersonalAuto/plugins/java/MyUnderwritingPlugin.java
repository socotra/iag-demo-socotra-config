package com.socotra.deployment.customer.personalauto;

import com.socotra.coremodel.*;
import com.socotra.deployment.customer.PersonalAutoQuoteRequest;
import com.socotra.deployment.customer.PersonalAutoRequest;
import com.socotra.deployment.customer.PersonalAutoQuote;
import com.socotra.deployment.customer.PersonalAutoSegment;
import com.socotra.deployment.customer.PersonalVehicleQuote;
import com.socotra.deployment.customer.PersonalVehicle;
import com.socotra.deployment.customer.UnderwritingPlugin;
import com.socotra.deployment.DataFetcher;
import com.socotra.platform.tools.ULID;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Dynamic underwriting plugin for Personal Auto quotes, driven by UWRules table.
 */
public class MyUnderwritingPlugin implements UnderwritingPlugin {
    private static final Logger log = LoggerFactory.getLogger(PersonalAutoUWPlugin.class);

    @Override
    public UnderwritingModification underwrite(PersonalAutoQuoteRequest request) {
        return underwriteAll(resourceSelector(request.quote()), request.quote());
    }

    @Override
    public UnderwritingModification underwrite(PersonalAutoRequest request) {
        PersonalAutoQuote quote = request.quote();
        ResourceSelector selector = resourceSelector(quote);
        if (request.segment().isEmpty()) {
            return defaultApprove(quote.locator());
        }
        PersonalAutoSegment segment = request.segment().get();
        return underwriteAll(selector, segment);
    }

    private UnderwritingModification underwriteAll(ResourceSelector selector, Object target) {
        List<UnderwritingFlagCore> flagsToCreate = new ArrayList<>();
        PersonalAutoQuote quote = (target instanceof PersonalAutoQuote)
                ? (PersonalAutoQuote) target
                : ((PersonalAutoSegment) target).quote();

        List<String> existingNotes = getNotesFromFlags(quote);

        for (int i = 1; i < 100; i++) {
            Optional<UWRules> ruleOpt = selector.getTable(UWRules.class)
                    .getRecord(UWRules.makeKey("uwRule", "PersonalAuto", String.valueOf(i)));
            if (ruleOpt.isEmpty()) {
                log.info("No more underwriting rules found after index {}", i);
                break;
            }
            UWRules rule = ruleOpt.get();
            boolean triggered = processUWRule(quote.element(), rule);
            log.info("Rule {} triggered: {}", rule.ruleId(), triggered);
            if (triggered && !existingNotes.contains(rule.message())) {
                flagsToCreate.add(createFlag(rule.decision(), rule.message()));
            }
        }
        if (flagsToCreate.isEmpty()) {
            flagsToCreate.add(createFlag("approve", "Risk is acceptable"));
        }
        return UnderwritingModification.builder()
                .flagsToCreate(flagsToCreate)
                .build();
    }

    private ResourceSelector resourceSelector(PersonalAutoQuote quote) {
        return ResourceSelectorFactory.getInstance().getSelector(quote);
    }

    private static List<String> getNotesFromFlags(PersonalAutoQuote quote) {
        var uwFlags = DataFetcher.getInstance().getQuoteUnderwritingFlags(quote.locator());
        List<UnderwritingFlag> combined = new ArrayList<>();
        if (uwFlags != null) {
            combined.addAll(Optional.ofNullable(uwFlags.clearedFlags()).orElse(Collections.emptyList()));
            combined.addAll(Optional.ofNullable(uwFlags.flags()).orElse(Collections.emptyList()));
        }
        return combined.stream()
                .map(f -> f.note().orElse(""))
                .toList();
    }

    private boolean processUWRule(Element element, UWRules rule) {
        String key = rule.key();
        String cond = rule.condition();
        String val = rule.value();
        String fieldValue = getFieldValue(element, key);
        log.info("Evaluating {} {} {} against value={}", key, cond, val, fieldValue);
        if (isInteger(fieldValue) && isInteger(val)) {
            return evalInteger(Integer.parseInt(fieldValue), cond, Integer.parseInt(val));
        }
        return evalString(fieldValue, cond, val);
    }

    private static String getFieldValue(Element element, String path) {
        var parts = Arrays.stream(path.split("\\."))
                .filter(s -> !s.isEmpty()).toList();
        return traverse(element, parts, 0);
    }

    private static String traverse(Element elem, List<String> keys, int idx) {
        if (idx == keys.size() - 1) {
            Object v = elem.data().get(keys.get(idx));
            return v != null ? v.toString() : null;
        }
        String key = keys.get(idx);
        for (Element child : elem.elements()) {
            if (child.type().equalsIgnoreCase(key + "Quote")) {
                String res = traverse(child, keys, idx + 1);
                if (res != null) return res;
            }
        }
        return null;
    }

    private UnderwritingFlagCore createFlag(String decision, String note) {
        UnderwritingLevel lvl = switch (decision.toLowerCase()) {
            case "reject" -> UnderwritingLevel.reject;
            case "decline" -> UnderwritingLevel.decline;
            case "block" -> UnderwritingLevel.block;
            case "info" -> UnderwritingLevel.info;
            case "approve" -> UnderwritingLevel.approve;
            default -> throw new IllegalArgumentException("Unknown decision: " + decision);
        };
        return UnderwritingFlagCore.builder()
                .level(lvl)
                .note(note)
                .build();
    }

    private static boolean evalInteger(int field, String cond, int ruleVal) {
        return switch (cond) {
            case "<" -> field < ruleVal;
            case ">" -> field > ruleVal;
            case "=" -> field == ruleVal;
            case "<=" -> field <= ruleVal;
            case ">=" -> field >= ruleVal;
            default -> throw new IllegalArgumentException("Unsupported condition: " + cond);
        };
    }

    private static boolean evalString(String field, String cond, String ruleVal) {
        return switch (cond.toLowerCase()) {
            case "=" -> field.equalsIgnoreCase(ruleVal);
            case "!=" -> !field.equalsIgnoreCase(ruleVal);
            case "in" -> Arrays.asList(ruleVal.split(",")).contains(field);
            case "not in" -> !Arrays.asList(ruleVal.split(",")).contains(field);
            default -> throw new IllegalArgumentException("Unsupported condition: " + cond);
        };
    }

    private static boolean isInteger(String s) {
        try { Integer.parseInt(s); return true; }
        catch (Exception e) { return false; }
    }
}
