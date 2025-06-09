package com.socotra.deployment.customer.personalauto;

import com.socotra.coremodel.*;
import com.socotra.deployment.DataFetcher;
import com.socotra.deployment.ResourceSelector;
import com.socotra.deployment.ResourceSelectorFactory;
import com.socotra.deployment.customer.UWRules;
import com.socotra.deployment.customer.UnderwritingPlugin;
import com.socotra.deployment.customer.PersonalAutoQuoteRequest;
import com.socotra.deployment.customer.PersonalAutoRequest;
import com.socotra.deployment.customer.PersonalAutoQuote;
import com.socotra.deployment.customer.PersonalAutoSegment;
import com.socotra.deployment.customer.PersonalVehicleQuote;
import com.socotra.deployment.customer.PersonalVehicle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;


public class MyAlternativeUnderwritingPlugin implements UnderwritingPlugin {
    private final BigDecimal BLOCK_THRESHOLD = BigDecimal.valueOf(100000);

    @Override
    public UnderwritingModification underwrite(PersonalAutoQuoteRequest request) {
        PersonalAutoQuote quote = request.quote();
        return underwriteVehicles(quote.personalVehicles(), quote.locator());
    }

    @Override
    public UnderwritingModification underwrite(PersonalAutoRequest request) {
        if (request.segment().isEmpty()) {
            return defaultApproval(request.quote().locator());
        }
        PersonalAutoSegment segment = request.segment().get();
        return underwriteVehicles(segment.personalVehicles(), segment.locator());
    }

    private UnderwritingModification underwriteVehicles(List<?> vehicles, ULID parentLocator) {
        List<UnderwritingFlagCore> flags = new ArrayList<>();

        for (Object obj : vehicles) {
            // Determine whether this is a quote-level vehicle or a policy-level vehicle
            BigDecimal value;
            String vin;
            String primaryUse;
            String damage;
            String suspendedLicense;
            String convictions;
            ULID locator;

            if (obj instanceof PersonalVehicleQuote) {
                PersonalVehicleQuote vq = (PersonalVehicleQuote) obj;
                var data = vq.data(); // PersonalVehicleQuote.PersonalVehicleQuoteData
                value = data.value();
                vin = data.vin();
                primaryUse = data.primaryVehicleUse();
                damage = data.vehicleDamage();
                suspendedLicense = data.suspendedLicense();
                convictions = data.convictions();
                locator = vq.locator();
            } else if (obj instanceof PersonalVehicle) {
                PersonalVehicle v = (PersonalVehicle) obj;
                var data = v.data(); // PersonalVehicle.PersonalVehicleData
                value = data.value();
                vin = data.vin();
                primaryUse = data.primaryVehicleUse();
                damage = data.vehicleDamage();
                suspendedLicense = data.suspendedLicense();
                convictions = data.convictions();
                locator = v.locator();
            } else {
                // Unknown type; skip
                continue;
            }

            // Rule 1: Vehicle value > 100,000 -> block
            if (value != null && value.compareTo(BLOCK_THRESHOLD) > 0) {
                flags.add(UnderwritingFlagCore.builder()
                        .level(UnderwritingLevel.block)
                        .note("Vehicle value exceeds 100,000 for VIN: " + vin)
                        .elementLocator(locator)
                        .build());
            }

            // Rule 2: Primary use = Business -> block
            if ("Business".equalsIgnoreCase(primaryUse)) {
                flags.add(UnderwritingFlagCore.builder()
                        .level(UnderwritingLevel.block)
                        .note("Primary vehicle use is Business for VIN: " + vin)
                        .elementLocator(locator)
                        .build());
            }

            // Rule 3: vehicleDamage = Hail Damage or Accident Damage -> info
            if ("Hail Damage".equalsIgnoreCase(damage) || "Accident Damage".equalsIgnoreCase(damage)) {
                flags.add(UnderwritingFlagCore.builder()
                        .level(UnderwritingLevel.info)
                        .note("Vehicle has reported damage: " + damage + " for VIN: " + vin)
                        .elementLocator(locator)
                        .build());
            }

            // Rule 4: Suspended license in last 5 years = YES -> decline
            if ("Yes".equalsIgnoreCase(suspendedLicense)) {
                flags.add(UnderwritingFlagCore.builder()
                        .level(UnderwritingLevel.decline)
                        .note("Driver had a suspended license in the last 5 years for VIN: " + vin)
                        .elementLocator(locator)
                        .build());
            }

            // Rule 5: Convicted of fraud/arson/theft = YES -> reject
            if ("Yes".equalsIgnoreCase(convictions)) {
                flags.add(UnderwritingFlagCore.builder()
                        .level(UnderwritingLevel.reject)
                        .note("Driver convicted of fraud, arson, or theft for VIN: " + vin)
                        .elementLocator(locator)
                        .build());
            }
        }

        // If no flags were generated, approve by default
        if (flags.isEmpty()) {
            flags.add(UnderwritingFlagCore.builder()
                    .level(UnderwritingLevel.approve)
                    .elementLocator(parentLocator)
                    .build());
        }

        return UnderwritingModification.builder()
                .flagsToCreate(flags)
                .build();
    }

    // Returns an UnderwritingModification that simply approves, used when there
    // are no vehicles to underwrite (e.g., empty segment).
    private UnderwritingModification defaultApproval(ULID locator) {
        return UnderwritingModification.builder()
                .flagsToCreate(List.of(
                        UnderwritingFlagCore.builder()
                                .level(UnderwritingLevel.approve)
                                .elementLocator(locator)
                                .build()))
                .build();
    }
}
