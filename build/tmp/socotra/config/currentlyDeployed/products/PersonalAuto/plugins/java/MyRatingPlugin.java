package com.socotra.deployment.customer.personalauto;
import com.socotra.deployment.customer.*;

import com.socotra.coremodel.RatingItem;
import com.socotra.coremodel.RatingSet;
import com.socotra.platform.tools.ULID;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;



public class MyRatingPlugin implements RatePlugin {

    private RatingItem rateBodilyInjury(PersonalVehicle vehicle) {
        double rate = 0.003 * vehicle.data().value().doubleValue() + 50;
        rate *= switch (vehicle.bodilyInjury().PALimit()) {
            case PAL5_000 -> 0.8;
            case PAL10_000 -> 0.82;
            case PAL20_000 -> 0.85;
            case PAL50_000 -> 0.87;
            case PAL100_000 -> 0.9;
            case PAL250_000 -> 0.92;
            case PAL500_000 -> 0.93;
            case PAL1_000_000 -> 1.05;
            case PAL2_000_000 -> 1.1;
            case PAL5_000_000 -> 2.0;
            default -> 1;
        };
        return RatingItem.builder()
            .elementLocator(vehicle.bodilyInjury().locator())
            .chargeType(ChargeType.premium)
            .rate(BigDecimal.valueOf(rate))
            .build();
    }

    private RatingItem rateUnderinsuredMotorist(PersonalVehicle vehicle){
        double rate = 0.002 * vehicle.data().value().doubleValue() + 50;
        rate *= switch (vehicle.underinsuredMotorist().PASplitLimit()) {
            case PA25_50 -> 0.9;
            case PA250_500 -> 0.91;
            case PA50_100 -> 0.92;
            case PA100_200 -> 0.95;
            case PA100_300 -> 0.95;
            default -> 1;
        };
        return RatingItem.builder()
            .elementLocator(vehicle.underinsuredMotorist().locator())
            .chargeType(ChargeType.premium)
            .rate(BigDecimal.valueOf(rate))
            .build();
    }

    private RatingItem rateUninsuredMotorist(PersonalVehicle vehicle){
        double rate = 0.002 * vehicle.data().value().doubleValue() + 50;
        rate *= switch (vehicle.uninsuredMotorist().PASplitLimit()) {
            case PA25_50 -> 0.9;
            case PA250_500 -> 0.91;
            case PA50_100 -> 0.92;
            case PA100_200 -> 0.95;
            case PA100_300 -> 0.95;
            default -> 1;
        };
        return RatingItem.builder()
            .elementLocator(vehicle.uninsuredMotorist().locator())
            .chargeType(ChargeType.premium)
            .rate(BigDecimal.valueOf(rate))
            .build();
    }

    private RatingItem rateRoadSideService(PersonalVehicle vehicle){
        double rate = 0.001 * vehicle.data().value().doubleValue() + 50;
        rate *= switch (vehicle.roadsideService().RSLimit()) {
            case RSL50 -> 0.92;
            case RSL100 -> 0.95;
            default -> 1;
        };
        return RatingItem.builder()
            .elementLocator(vehicle.roadsideService().locator())
            .chargeType(ChargeType.premium)
            .rate(BigDecimal.valueOf(rate))
            .build();
    }

    private RatingItem rateCollision(PersonalVehicle vehicle){
        double rate = 0.001 * vehicle.data().value().doubleValue() + 50;
        rate *= switch (vehicle.collision().PADeductible()) {
            case PAD100 -> 0.95;
            case PAD250 -> 0.94;
            case PAD500 -> 0.93;
            case PAD750 -> 0.92;
            case PAD1000 -> 0.9;
            case PAD1500 -> 0.88;
            case PAD2000 -> 0.85;
            default -> 1;
        };
        return RatingItem.builder()
            .elementLocator(vehicle.collision().locator())
            .chargeType(ChargeType.premium)
            .rate(BigDecimal.valueOf(rate))
            .build();
    }

    private RatingItem rateComprehensive(PersonalVehicle vehicle){
        double rate = 0.001 * vehicle.data().value().doubleValue() + 50;
        rate *= switch (vehicle.comprehensive().PADeductible()) {
            case PAD100 -> 0.95;
            case PAD250 -> 0.94;
            case PAD500 -> 0.93;
            case PAD750 -> 0.92;
            case PAD1000 -> 0.9;
            case PAD1500 -> 0.88;
            case PAD2000 -> 0.85;
            default -> 1;
        };
        return RatingItem.builder()
            .elementLocator(vehicle.comprehensive().locator())
            .chargeType(ChargeType.premium)
            .rate(BigDecimal.valueOf(rate))
            .build();
    }

    private RatingItem ratePropertyDamage(PersonalVehicle vehicle){
        double rate = 0.001 * vehicle.data().value().doubleValue() + 50;
        rate *= switch (vehicle.propertyDamage().PALimit()) {
            case PAL5_000 -> 0.8;
            case PAL10_000 -> 0.82;
            case PAL20_000 -> 0.85;
            case PAL50_000 -> 0.87;
            case PAL100_000 -> 0.9;
            case PAL250_000 -> 0.92;
            case PAL500_000 -> 0.93;
            case PAL1_000_000 -> 1.05;
            case PAL2_000_000 -> 1.1;
            case PAL5_000_000 -> 2.0;
            default -> 1;
        };
        return RatingItem.builder()
            .elementLocator(vehicle.propertyDamage().locator())
            .chargeType(ChargeType.premium)
            .rate(BigDecimal.valueOf(rate))
            .build();
    }

    private RatingItem rateMedicalPayments(PersonalVehicle vehicle){
        double rate = 0.003 * vehicle.data().value().doubleValue() + 50;
        rate *= switch (vehicle.medicalPayments().PALimit()) {
            case PAL5_000 -> 0.8;
            case PAL10_000 -> 0.82;
            case PAL20_000 -> 0.85;
            case PAL50_000 -> 0.87;
            case PAL100_000 -> 0.9;
            case PAL250_000 -> 0.92;
            case PAL500_000 -> 0.93;
            case PAL1_000_000 -> 1.05;
            case PAL2_000_000 -> 1.1;
            case PAL5_000_000 -> 2.0;
            default -> 1;
        };
        return RatingItem.builder()
            .elementLocator(vehicle.medicalPayments().locator())
            .chargeType(ChargeType.premium)
            .rate(BigDecimal.valueOf(rate))
            .build();
    }

    private RatingItem rateTax(ULID locator, List<RatingItem> ratingItems) {

        BigDecimal sum = BigDecimal.ZERO;
        for (RatingItem item : ratingItems) {
            sum = sum.add(item.rate().orElse(BigDecimal.ZERO));
        }

        return RatingItem.builder()
            .elementLocator(locator)
            .chargeType(ChargeType.GST)
            .rate(sum.multiply(BigDecimal.valueOf(0.0525)))
            .build();
    }

    private RatingItem rateFee(ULID locator, BigDecimal duration) {
        return RatingItem.builder()
            .elementLocator(locator)
            .chargeType(ChargeType.fee)
            .rate(BigDecimal.valueOf(10L / duration.doubleValue()))
            .build();
    }

    @Override
    public RatingSet rate(PersonalAutoQuoteRequest request) {
        PersonalAutoQuote personalAutoQuote = request.quote();
        BigDecimal duration = request.duration();
        List<RatingItem> ratingItems = new ArrayList<>();
        for(PersonalVehicle vehicle : personalAutoQuote.personalVehicles()) {
            if (vehicle.bodilyInjury() != null) {
                ratingItems.add(rateBodilyInjury(vehicle));
            }
            if (vehicle.collision() != null) {
                ratingItems.add(rateCollision(vehicle));
            }
            if (vehicle.comprehensive() != null) {
                ratingItems.add(rateComprehensive(vehicle));
            }
            // adding additional coverages to increase premium
            if (vehicle.roadsideService() != null) {
                ratingItems.add(rateRoadSideService(vehicle));
            }
            if (vehicle.propertyDamage() != null) {
                ratingItems.add(ratePropertyDamage(vehicle));
            }
            if (vehicle.underinsuredMotorist() != null) {
                ratingItems.add(rateUnderinsuredMotorist(vehicle));
            }
            if (vehicle.uninsuredMotorist() != null) {
                ratingItems.add(rateUninsuredMotorist(vehicle));
            }
            if (vehicle.medicalPayments() != null) {
                ratingItems.add(rateMedicalPayments(vehicle));
            }
        }

        ratingItems.add(rateFee(request.quote().locator(), duration));
        if (ratingItems.size() > 0) {
            ratingItems.add(rateTax(request.quote().locator(), ratingItems));
        }

        return RatingSet.builder().ok(true).ratingItems(ratingItems).build();
    }

    @Override
    public RatingSet rate(PersonalAutoRequest request) {
        BigDecimal duration = request.duration();
        List<RatingItem> ratingItems = new ArrayList<>();
        List<PersonalVehicle> personalVehicles = new ArrayList<>();
        request.segment().ifPresent(s -> personalVehicles.addAll(s.personalVehicles()));
        for(PersonalVehicle vehicle : personalVehicles){
            if(vehicle.bodilyInjury() != null) {
                ratingItems.add(rateBodilyInjury(vehicle));
            }
            if(vehicle.collision() != null) {
                ratingItems.add(rateCollision(vehicle));
            }
            if(vehicle.comprehensive() != null) {
                ratingItems.add(rateComprehensive(vehicle));
            }
            // adding additional coverages to increase premium
            if(vehicle.roadsideService() != null) {
                ratingItems.add(rateRoadSideService(vehicle));
            }
            if(vehicle.propertyDamage() != null) {
                ratingItems.add(ratePropertyDamage(vehicle));
            }
            if(vehicle.underinsuredMotorist() != null) {
                ratingItems.add(rateUnderinsuredMotorist(vehicle));
            }
            if(vehicle.uninsuredMotorist() != null) {
                ratingItems.add(rateUninsuredMotorist(vehicle));
            }
            if(vehicle.medicalPayments() != null) {
                ratingItems.add(rateMedicalPayments(vehicle));
            }

        }

        request.segment().ifPresent(s-> ratingItems.add(rateFee(s.locator(), duration)));
        if (ratingItems.size() > 0) {
            request.segment().ifPresent(s-> ratingItems.add(rateTax(s.locator(), ratingItems)));
        }

        return RatingSet.builder().ok(true).ratingItems(ratingItems).build();
    }
}