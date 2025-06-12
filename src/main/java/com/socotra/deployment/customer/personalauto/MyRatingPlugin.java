package com.socotra.deployment.customer.personalauto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socotra.deployment.customer.*;

import com.socotra.coremodel.RatingItem;
import com.socotra.coremodel.RatingSet;
import com.socotra.platform.tools.ULID;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MyRatingPlugin implements RatePlugin {

    private static final String LAMBDA_URL = "https://bjh5pm3o72nwzzti3tqfl3ekbi0eebcd.lambda-url.us-east-2.on.aws/";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RatingSet callRemoteLambda(Object requestPayload) {
        try {
            URL url = new URL(LAMBDA_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Wrap payload in { "body": "<stringified JSON>" }
            Map<String, String> bodyWrapper = new HashMap<>();
            bodyWrapper.put("body", MAPPER.writeValueAsString(requestPayload));
            String finalJson = MAPPER.writeValueAsString(bodyWrapper);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(finalJson.getBytes());
            }

            if (connection.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + connection.getResponseCode());
            }

            JsonNode root = MAPPER.readTree(connection.getInputStream());
            Map <String, Object> result = MAPPER.treeToValue(root, Map.class);
            return (RatingSet) result.get("ratingSet");

        } catch (Exception e) {
            throw new RuntimeException("Error calling Lambda: " + e.getMessage(), e);
        }
    }

    private RatingItem flatRateWithTaxes(ULID locator, double baseAmount) {
        double total =
                baseAmount +                  // Base premium
                        (baseAmount * 0.08) +         // FSL (8%)
                        (baseAmount * 0.10) +         // GST (10%)
                        (baseAmount * 0.09);          // SD (9%)

        return RatingItem.builder()
                .elementLocator(locator)
                .chargeType(ChargeType.premium)  // Keep using 'premium' as combined total
                .rate(BigDecimal.valueOf(total))
                .build();
    }

    private double getRatingFactor(PersonalVehicle vehicle, PersonalAuto quote, String coverageType) {
        double stateFactor = switch (vehicle.data().vehicleLicenseState()) {
            case "NSW" -> switch (coverageType) {
                case "Fire" -> 1.1;
                case "Theft" -> 1.2;
                case "OwnDamage" -> 1.3;
                case "ThirdParty" -> 1.4;
                default -> 1.0;
            };
            case "WA" -> switch (coverageType) {
                case "Fire" -> 1.2;
                case "Theft" -> 1.3;
                case "OwnDamage" -> 1.4;
                case "ThirdParty" -> 1.5;
                default -> 1.0;
            };
            case "QLD" -> switch (coverageType) {
                case "Fire" -> 1.3;
                case "Theft" -> 1.4;
                case "OwnDamage" -> 1.5;
                case "ThirdParty" -> 1.6;
                default -> 1.0;
            };
            case "VIC" -> switch (coverageType) {
                case "Fire" -> 1.4;
                case "Theft" -> 1.5;
                case "OwnDamage" -> 1.6;
                case "ThirdParty" -> 1.7;
                default -> 1.0;
            };
            case "NT" -> switch (coverageType) {
                case "Fire" -> 1.5;
                case "Theft" -> 1.6;
                case "OwnDamage" -> 1.7;
                case "ThirdParty" -> 1.8;
                default -> 1.0;
            };
            case "ACT" -> switch (coverageType) {
                case "Fire" -> 1.6;
                case "Theft" -> 1.7;
                case "OwnDamage" -> 1.8;
                case "ThirdParty" -> 1.9;
                default -> 1.0;
            };
            case "TAS" -> switch (coverageType) {
                case "Fire" -> 1.7;
                case "Theft" -> 1.8;
                case "OwnDamage" -> 1.9;
                case "ThirdParty" -> 2.0;
                default -> 1.0;
            };
            case "SA" -> switch (coverageType) {
                case "Fire" -> 1.8;
                case "Theft" -> 1.9;
                case "OwnDamage" -> 2.0;
                case "ThirdParty" -> 2.1;
                default -> 1.0;
            };
            default -> 1.0;
        };

        double damageFactor = switch (vehicle.data().vehicleDamage()) {
            case "No Damage" -> switch (coverageType) {
                case "Fire" -> 1.0;
                case "Theft" -> 1.1;
                case "OwnDamage" -> 1.2;
                case "ThirdParty" -> 1.3;
                default -> 1.0;
            };
            case "Hail Damage" -> switch (coverageType) {
                case "Fire" -> 1.5;
                case "Theft" -> 1.6;
                case "OwnDamage" -> 1.7;
                case "ThirdParty" -> 1.8;
                default -> 1.0;
            };
            case "Accident Damage" -> switch (coverageType) {
                case "Fire" -> 2.0;
                case "Theft" -> 2.1;
                case "OwnDamage" -> 2.2;
                case "ThirdParty" -> 2.3;
                default -> 1.0;
            };
            default -> 1.0;
        };

        double licenceFactor = switch (vehicle.data().primaryDriverLicence()) {
            case "Learner Permit or Licence" -> switch (coverageType) {
                case "Fire" -> 1.9;
                case "Theft" -> 2.0;
                case "OwnDamage" -> 2.1;
                case "ThirdParty" -> 2.2;
                default -> 1.0;
            };
            case "Provisional/Probationary/Restricted licence" -> switch (coverageType) {
                case "Fire" -> 1.7;
                case "Theft" -> 1.8;
                case "OwnDamage" -> 1.9;
                case "ThirdParty" -> 2.0;
                default -> 1.0;
            };
            case "Full/Open licence" -> switch (coverageType) {
                case "Fire" -> 1.0;
                case "Theft" -> 1.1;
                case "OwnDamage" -> 2.2;
                case "ThirdParty" -> 2.3;
                default -> 1.0;
            };
            case "International licence" -> switch (coverageType) {
                case "Fire" -> 1.5;
                case "Theft" -> 1.7;
                case "OwnDamage" -> 2.8;
                case "ThirdParty" -> 1.2;
                default -> 1.0;
            };
            default -> 1.0;
        };

        double excessFactor = switch (vehicle.data().excess()) {
            case "100" -> switch (coverageType) {
                case "Fire" -> 2.0;
                case "Theft" -> 2.1;
                case "OwnDamage" -> 1.8;
                case "ThirdParty" -> 1.5;
                default -> 1.0;
            };
            case "200" -> switch (coverageType) {
                case "Fire" -> 1.9;
                case "Theft" -> 2.0;
                case "OwnDamage" -> 1.7;
                case "ThirdParty" -> 1.4;
                default -> 1.0;
            };
            case "300" -> switch (coverageType) {
                case "Fire" -> 1.8;
                case "Theft" -> 1.9;
                case "OwnDamage" -> 1.6;
                case "ThirdParty" -> 1.3;
                default -> 1.0;
            };
            case "500" -> switch (coverageType) {
                case "Fire" -> 1.7;
                case "Theft" -> 1.8;
                case "OwnDamage" -> 1.5;
                case "ThirdParty" -> 1.2;
                default -> 1.0;
            };
            case "600" -> switch (coverageType) {
                case "Fire" -> 1.6;
                case "Theft" -> 1.7;
                case "OwnDamage" -> 1.4;
                case "ThirdParty" -> 1.1;
                default -> 1.0;
            };
            case "900" -> switch (coverageType) {
                case "Fire" -> 1.0;
                case "Theft" -> 1.6;
                case "OwnDamage" -> 1.3;
                case "ThirdParty" -> 1.0;
                default -> 1.0;
            };
            case "1000" -> switch (coverageType) {
                case "Fire" -> 0.9;
                case "Theft" -> 1.5;
                case "OwnDamage" -> 1.2;
                case "ThirdParty" -> 0.9;
                default -> 1.0;
            };
            case "1500" -> switch (coverageType) {
                case "Fire" -> 0.8;
                case "Theft" -> 1.4;
                case "OwnDamage" -> 1.1;
                case "ThirdParty" -> 0.8;
                default -> 1.0;
            };
            default -> 1.0;
        };

        double usageFactor = switch (vehicle.data().vehicleUsage()) {
            case "1-2" -> switch (coverageType) {
                case "Theft" -> 1.5;
                case "OwnDamage" -> 1.6;
                case "ThirdParty" -> 1.7;
                default -> 1.8;
            };
            case "3-4" -> switch (coverageType) {
                case "Theft" -> 1.4;
                case "OwnDamage" -> 1.3;
                case "ThirdParty" -> 1.2;
                default -> 1.1;
            };
            case "5+" -> switch (coverageType) {
                case "Theft" -> 1.6;
                case "OwnDamage" -> 1.5;
                case "ThirdParty" -> 1.4;
                default -> 1.3;
            };
            default -> 1.0;
        };

        int vehicleYear = vehicle.data().year() != null ? vehicle.data().year() : 2020;
        double yearFactor = switch (vehicleYear) {
            case 2024 -> switch (coverageType) {
                case "Theft" -> 2.1;
                case "OwnDamage" -> 3.1;
                case "ThirdParty" -> 3.1;
                default -> 2.1; // Relativity
            };
            case 2023 -> switch (coverageType) {
                case "Theft" -> 2.0;
                case "OwnDamage" -> 3.0;
                case "ThirdParty" -> 3.0;
                default -> 2.0;
            };
            case 2022 -> switch (coverageType) {
                case "Theft" -> 1.9;
                case "OwnDamage" -> 2.9;
                case "ThirdParty" -> 2.9;
                default -> 1.9;
            };
            case 2021 -> switch (coverageType) {
                case "Theft" -> 1.8;
                case "OwnDamage" -> 2.8;
                case "ThirdParty" -> 2.8;
                default -> 1.8;
            };
            case 2020 -> switch (coverageType) {
                case "Theft" -> 1.7;
                case "OwnDamage" -> 2.7;
                case "ThirdParty" -> 2.7;
                default -> 1.7;
            };
            case 2019 -> switch (coverageType) {
                case "Theft" -> 1.6;
                case "OwnDamage" -> 2.6;
                case "ThirdParty" -> 2.6;
                default -> 1.6;
            };
            case 2018 -> switch (coverageType) {
                case "Theft" -> 1.5;
                case "OwnDamage" -> 2.5;
                case "ThirdParty" -> 2.5;
                default -> 1.5;
            };
            case 2017 -> switch (coverageType) {
                case "Theft" -> 1.4;
                case "OwnDamage" -> 2.4;
                case "ThirdParty" -> 2.4;
                default -> 1.4;
            };
            case 2016 -> switch (coverageType) {
                case "Theft" -> 1.3;
                case "OwnDamage" -> 2.3;
                case "ThirdParty" -> 2.3;
                default -> 1.3;
            };
            case 2015 -> switch (coverageType) {
                case "Theft" -> 1.2;
                case "OwnDamage" -> 2.2;
                case "ThirdParty" -> 2.2;
                default -> 1.2;
            };
            case 2014 -> switch (coverageType) {
                case "Theft" -> 1.1;
                case "OwnDamage" -> 2.1;
                case "ThirdParty" -> 2.1;
                default -> 1.1;
            };
            case 2013 -> switch (coverageType) {
                case "Theft" -> 1.0;
                case "OwnDamage" -> 2.0;
                case "ThirdParty" -> 2.0;
                default -> 1.0;
            };
            case 2012 -> switch (coverageType) {
                case "Theft" -> 0.9;
                case "OwnDamage" -> 1.9;
                case "ThirdParty" -> 1.9;
                default -> 0.9;
            };
            case 2011 -> switch (coverageType) {
                case "Theft" -> 0.8;
                case "OwnDamage" -> 1.8;
                case "ThirdParty" -> 1.8;
                default -> 0.8;
            };
            case 2010 -> switch (coverageType) {
                case "Theft" -> 0.7;
                case "OwnDamage" -> 1.7;
                case "ThirdParty" -> 1.7;
                default -> 0.7;
            };
            default -> {
                if (vehicleYear < 2010) {
                    yield switch (coverageType) {
                        case "Theft" -> 0.6;
                        case "OwnDamage" -> 1.6;
                        case "ThirdParty" -> 1.6;
                        default -> 0.6;
                    };
                } else {
                    yield 1.0;
                }
            }
        };

        LocalDate dob = vehicle.data().drivers() != null && vehicle.data().drivers().isEmpty() ? vehicle.data().drivers().get(0).dateOfBirth() : null; //Assume first driver is primary driver
        int driverLicenseAge = dob != null ? Period.between(dob, LocalDate.now()).getYears() - 17 : 5; // Assumes license at 17
        if (driverLicenseAge < 1) driverLicenseAge = 1; // Prevent negative values

        double driverLicenseAgeFactor = switch (driverLicenseAge) {
            case 1 -> switch (coverageType) {
                case "Theft" -> 1.5;
                case "OwnDamage" -> 1.9;
                case "ThirdParty" -> 1.5;
                default -> 1.5;
            };
            case 2 -> switch (coverageType) {
                case "Theft" -> 1.4;
                case "OwnDamage" -> 1.8;
                case "ThirdParty" -> 1.4;
                default -> 1.4;
            };
            case 3 -> switch (coverageType) {
                case "Theft" -> 1.4;
                case "OwnDamage" -> 1.7;
                case "ThirdParty" -> 1.4;
                default -> 1.4;
            };
            case 4 -> switch (coverageType) {
                case "Theft" -> 1.4;
                case "OwnDamage" -> 1.6;
                case "ThirdParty" -> 1.4;
                default -> 1.4;
            };
            case 5 -> switch (coverageType) {
                case "Theft" -> 1.4;
                case "OwnDamage" -> 1.5;
                case "ThirdParty" -> 1.4;
                default -> 1.4;
            };
            case 6 -> switch (coverageType) {
                case "Theft" -> 1.3;
                case "OwnDamage" -> 1.4;
                case "ThirdParty" -> 1.3;
                default -> 1.3;
            };
            case 7 -> switch (coverageType) {
                case "Theft" -> 1.3;
                case "OwnDamage" -> 1.3;
                case "ThirdParty" -> 1.3;
                default -> 1.3;
            };
            case 8 -> switch (coverageType) {
                case "Theft" -> 1.3;
                case "OwnDamage" -> 1.2;
                case "ThirdParty" -> 1.3;
                default -> 1.3;
            };
            case 9 -> switch (coverageType) {
                case "Theft" -> 1.3;
                case "OwnDamage" -> 1.1;
                case "ThirdParty" -> 1.3;
                default -> 1.3;
            };
            case 10 -> switch (coverageType) {
                case "Theft" -> 1.2;
                case "OwnDamage" -> 1.0;
                case "ThirdParty" -> 1.2;
                default -> 1.2;
            };
            default -> switch (coverageType) {
                case "Theft" -> 1.3;
                case "OwnDamage" -> 0.9;
                case "ThirdParty" -> 1.3;
                default -> 1.3;
            };
        };

        // Parse the priorClaims field (string) into an integer count
        int claims = 0;
        try {
            String claimStr = quote.data().claims();
            claims = claimStr.equals("5+") ? 5 : Integer.parseInt(claimStr);
        } catch (Exception e) {
            // If for some reason priorClaims is null or non‐numeric, default to 0
            claims = 0;
        }

        // Apply coverage‐specific relativity from your table
        double claimsFactor = switch (claims) {
            case 0 -> switch (coverageType) {
                case "Theft"     -> 1.2;
                case "OwnDamage" -> 1.3;
                case "ThirdParty"-> 1.4;
                default          -> 1.1;  // generic “Relativity” column
            };
            case 1 -> switch (coverageType) {
                case "Theft"     -> 1.3;
                case "OwnDamage" -> 1.4;
                case "ThirdParty"-> 1.5;
                default          -> 1.2;
            };
            case 2 -> switch (coverageType) {
                case "Theft"     -> 1.4;
                case "OwnDamage" -> 1.5;
                case "ThirdParty"-> 1.6;
                default          -> 1.3;
            };
            case 3 -> switch (coverageType) {
                case "Theft"     -> 1.5;
                case "OwnDamage" -> 1.6;
                case "ThirdParty"-> 1.7;
                default          -> 1.4;
            };
            case 4 -> switch (coverageType) {
                case "Theft"     -> 1.6;
                case "OwnDamage" -> 1.7;
                case "ThirdParty"-> 1.8;
                default          -> 1.5;
            };
            default -> {
                // Any count ≥5 (“5+”) falls into this case:
                yield switch (coverageType) {
                    case "Theft"     -> 2.8;
                    case "OwnDamage" -> 2.9;
                    case "ThirdParty"-> 2.9;
                    default          -> 2.9;
                };
            }
        };

        return stateFactor * damageFactor * licenceFactor * excessFactor *
                usageFactor * yearFactor * driverLicenseAgeFactor * claimsFactor;
    }

    private RatingItem rateFire(PersonalVehicle vehicle, PersonalAuto quote) {
        double baseRate = 100.0;
        double finalRate = baseRate * getRatingFactor(vehicle, quote,"Fire");
        return RatingItem.builder()
                .elementLocator(vehicle.fire().locator())
                .chargeType(ChargeType.premium)
                .rate(BigDecimal.valueOf(finalRate))
                .build();
    }

    private RatingItem rateTheft(PersonalVehicle vehicle, PersonalAuto quote) {
        double baseRate = 100.0;
        double finalRate = baseRate * getRatingFactor(vehicle, quote, "Theft");
        return RatingItem.builder()
                .elementLocator(vehicle.theft().locator())
                .chargeType(ChargeType.premium)
                .rate(BigDecimal.valueOf(finalRate))
                .build();
    }

    private RatingItem rateOwnDamage(PersonalVehicle vehicle, PersonalAuto quote) {
        double baseRate = 200.0;
        double finalRate = baseRate * getRatingFactor(vehicle, quote, "OwnDamage");
        return RatingItem.builder()
                .elementLocator(vehicle.ownDamage().locator())
                .chargeType(ChargeType.premium)
                .rate(BigDecimal.valueOf(finalRate))
                .build();
    }

    private RatingItem rateThirdParty(PersonalVehicle vehicle, PersonalAuto quote) {
        double baseRate = 80.0;
        double finalRate = baseRate * getRatingFactor(vehicle, quote, "ThirdParty");
        return RatingItem.builder()
                .elementLocator(vehicle.thirdParty().locator())
                .chargeType(ChargeType.premium)
                .rate(BigDecimal.valueOf(finalRate))
                .build();
    }

    private RatingItem rateWindscreen(PersonalVehicle vehicle) {
        return RatingItem.builder()
                .elementLocator(vehicle.windscreen().locator())
                .chargeType(ChargeType.premium)
                .rate(BigDecimal.valueOf(20))
                .build();
    }

    private RatingItem rateBabySeat(PersonalVehicle vehicle) {
        return RatingItem.builder()
                .elementLocator(vehicle.babySeat().locator())
                .chargeType(ChargeType.premium)
                .rate(BigDecimal.valueOf(20))
                .build();
    }

    @Override
    public RatingSet rate(PersonalAutoQuoteRequest request) {
        return callRemoteLambda(request);
    }

    @Override
    public RatingSet rate(PersonalAutoRequest request) {
        return callRemoteLambda(request);
    }
}