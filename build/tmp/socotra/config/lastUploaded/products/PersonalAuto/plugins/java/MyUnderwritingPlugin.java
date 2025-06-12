package com.socotra.deployment.customer.personalauto;
import com.socotra.deployment.customer.*;

import com.socotra.coremodel.UnderwritingFlagCore;
import com.socotra.coremodel.UnderwritingFlags;
import com.socotra.coremodel.UnderwritingLevel;
import com.socotra.coremodel.UnderwritingModification;
import com.socotra.deployment.DataFetcher;
import com.socotra.platform.tools.ULID;

import java.math.BigDecimal;
import java.util.List;

public class MyUnderwritingPlugin implements UnderwritingPlugin {
    private final BigDecimal VALUE_THRESHOLD = BigDecimal.valueOf(100000);
    private final BigDecimal REJECT_THRESHOLD = BigDecimal.valueOf(300000);

    @Override
    public UnderwritingModification underwrite(PersonalAutoQuoteRequest request){
        PersonalAutoQuote quote = request.quote();
        UnderwritingFlags fetchedFlags = DataFetcher.getInstance().getQuoteUnderwritingFlags(quote.locator());
        BigDecimal maxValue = BigDecimal.ZERO;
        String vin = "";
        ULID elementLocator = null;
        for(PersonalVehicleQuote vq: quote.personalVehicles()) {
            PersonalVehicleQuote.PersonalVehicleQuoteData data = vq.data();
            if(maxValue.compareTo(data.value()) < 0) {
                maxValue = data.value();
                vin = data.vin();
                elementLocator = vq.locator();
            }
        }
        return getUnderwritingModification(maxValue, vin, elementLocator);
    }

    public UnderwritingModification underwrite(PersonalAutoRequest request){
        if (request.segment().isEmpty()) {
            return UnderwritingModification.builder()
                    .flagsToCreate(List.of(UnderwritingFlagCore.builder()
                            .level(UnderwritingLevel.approve)
                            .build()))
                    .build();
        }

        PersonalAutoSegment segment = request.segment().get();
        UnderwritingFlags fetchedFlags = DataFetcher.getInstance().getQuoteUnderwritingFlags(segment.locator());
        BigDecimal maxValue = BigDecimal.ZERO;
        String vin = "";
        ULID elementLocator = null;
        for(var vq: segment.personalVehicles()) {
            var data = vq.data();
            if(maxValue.compareTo(data.value()) < 0) {
                maxValue = data.value();
                vin = data.vin();
                elementLocator = vq.locator();
            }
        }
        return getUnderwritingModification(maxValue, vin, elementLocator);
    }

    private UnderwritingModification getUnderwritingModification(BigDecimal maxValue, String vin, ULID elementLocator) {
        if(maxValue.compareTo(REJECT_THRESHOLD) >= 0) {
            return UnderwritingModification.builder()
                    .flagsToCreate(List.of(UnderwritingFlagCore.builder()
                            .level(UnderwritingLevel.reject)
                            .note("Vehicle's value is above the threshold of " + REJECT_THRESHOLD.toString() + " for VIN " + vin)
                            .elementLocator(elementLocator)
                            .build()))
                    .build();
        }
        if(maxValue.compareTo(VALUE_THRESHOLD) >= 0) {
            return UnderwritingModification.builder()
                    .flagsToCreate(List.of(UnderwritingFlagCore.builder()
                            .level(UnderwritingLevel.decline)
                            .note("Vehicle's value is above the threshold of " + VALUE_THRESHOLD.toString() + " for VIN " + vin)
                            .elementLocator(elementLocator)
                            .build()))
                    .build();
        }
        return UnderwritingModification.builder()
                .flagsToCreate(List.of(UnderwritingFlagCore.builder()
                        .level(UnderwritingLevel.approve)
                        .build()))
                .build();
    }
}