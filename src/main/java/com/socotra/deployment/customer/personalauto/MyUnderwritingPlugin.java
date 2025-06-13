package com.socotra.deployment.customer.personalauto;

import com.socotra.coremodel.*;
import com.socotra.deployment.ResourceSelector;
import com.socotra.deployment.ResourceSelectorFactory;
import com.socotra.deployment.DataFetcher;
import com.socotra.deployment.customer.*;
import com.socotra.platform.tools.ULID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MyUnderwritingPlugin implements UnderwritingPlugin {
    private static final Logger log = LoggerFactory.getLogger(MyUnderwritingPlugin.class);

    @Override
    public UnderwritingModification underwrite(PersonalAutoQuoteRequest request) {
        return underwriteAll(
                ResourceSelectorFactory.getInstance().getSelector(request.quote()),
                request.quote()
        );
    }

    @Override
    public UnderwritingModification underwrite(PersonalAutoRequest request) {
        if (request.segment().isEmpty()) {
            return defaultApprove("<unknown>");
        }
        PersonalAutoSegment seg = request.segment().get();
        return underwriteAll(
                ResourceSelectorFactory.getInstance().getSelector(seg),
                seg
        );
    }

    private UnderwritingModification underwriteAll(ResourceSelector selector, Object target) {
        List<UnderwritingFlagCore> flags = new ArrayList<>();
        PersonalAuto quote = (target instanceof PersonalAutoQuote)
                ? (PersonalAutoQuote) target
                : (PersonalAutoSegment) target;

        Set<String> existingNotes = getNotesFromFlags(quote);

        for (int i = 1; i < 100; i++) {
            Optional<UWRules> r = selector.getTable(UWRules.class)
                    .getRecord(UWRules.makeKey("uwRule", "PersonalAuto", i));
            if (r.isEmpty()) break;

            UWRules rule = r.get();
            boolean triggered = processUWRule(
                    quote instanceof PersonalAutoQuote
                            ? ((PersonalAutoQuote)quote).element()
                            : ((PersonalAutoSegment)quote).element(),
                    rule
            );
            log.info("Rule {} triggered: {}", rule.ruleId(), triggered);
            if (triggered && !existingNotes.contains(rule.message())) {
                flags.add(createFlag(rule.decision(), rule.message()));
            }
        }

        if (flags.isEmpty()) {
            flags.add(createFlag("approve", "Risk is acceptable"));
        }

        return UnderwritingModification.builder()
                .flagsToCreate(flags)
                .build();
    }

    private static Set<String> getNotesFromFlags(PersonalAuto quote) {
        var uw = DataFetcher.getInstance().getQuoteUnderwritingFlags(quote.locator());
        List<UnderwritingFlag> all = new ArrayList<>();
        if (uw != null) {
            all.addAll(Optional.ofNullable(uw.clearedFlags()).orElse(List.of()));
            all.addAll(Optional.ofNullable(uw.flags()).orElse(List.of()));
        }
        var notes = new HashSet<String>();
        for (var f : all) f.note().ifPresent(notes::add);
        return notes;
    }

    private boolean processUWRule(Element element, UWRules rule) {
        String key  = rule.key();
        String cond = rule.condition();
        String val  = rule.value();
        String field = getFieldValue(element, key);
        log.info("Evaluating {} {} {} against value={}", key, cond, val, field);

        // if it's a numeric comparator, always try integer compare
        if (List.of("<", ">", "<=", ">=", "=").contains(cond)) {
            try {
                int fv = Integer.parseInt(field);
                int rv = Integer.parseInt(val);
                return evalInteger(fv, cond, rv);
            } catch (Exception e) {
                // missing or non-numeric field ⇒ rule does not trigger
                return false;
            }
        }
        // otherwise string operator
        return evalString(field, cond, val);
    }

    private static String getFieldValue(Element e, String path) {
        var parts = Arrays.stream(path.split("\\."))
                .filter(s->!s.isEmpty())
                .toList();
        return traverse(e, parts, 0);
    }

    private static String traverse(Element elem, List<String> keys, int idx) {
        if (idx == keys.size() - 1) {
            Object v = elem.data().get(keys.get(idx));
            return v == null ? null : v.toString();
        }
        String key = keys.get(idx);
        for (Element child : elem.elements()) {
            if (child.type().equalsIgnoreCase(key + "Quote")) {
                String r = traverse(child, keys, idx + 1);
                if (r != null) return r;
            }
        }
        return null;
    }

    private UnderwritingFlagCore createFlag(String decision, String note) {
        UnderwritingLevel lvl = switch (decision.toLowerCase()) {
            case "reject"   -> UnderwritingLevel.reject;
            case "decline"  -> UnderwritingLevel.decline;
            case "block"    -> UnderwritingLevel.block;
            case "info"     -> UnderwritingLevel.info;
            case "approve"  -> UnderwritingLevel.approve;
            default         -> throw new IllegalArgumentException("Unknown decision: " + decision);
        };
        return UnderwritingFlagCore.builder()
                .level(lvl)
                .note(note)
                .build();
    }

    private static boolean evalInteger(int field, String cond, int ruleVal) {
        return switch (cond) {
            case "<"  -> field <  ruleVal;
            case ">"  -> field >  ruleVal;
            case "="  -> field == ruleVal;
            case "<=" -> field <= ruleVal;
            case ">=" -> field >= ruleVal;
            default   -> false;
        };
    }

    private static boolean evalString(String field, String cond, String ruleVal) {
        if (field == null) return false;          // no value ⇒ no match
        return switch (cond.toLowerCase()) {
            case "="     -> field.equalsIgnoreCase(ruleVal);
            case "!="    -> !field.equalsIgnoreCase(ruleVal);
            case "in"    -> Set.of(ruleVal.split(",")).contains(field);
            case "not in"-> !Set.of(ruleVal.split(",")).contains(field);
            default      -> false;               // unknown string op ⇒ no match
        };
    }

    private UnderwritingModification defaultApprove(String locator) {
        return UnderwritingModification.builder()
                .flagsToCreate(List.of(
                        UnderwritingFlagCore.builder()
                                .level(UnderwritingLevel.approve)
                                .note("Default approval: " + locator)
                                .build()
                ))
                .build();
    }
}