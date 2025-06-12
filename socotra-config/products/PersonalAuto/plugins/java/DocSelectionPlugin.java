package com.socotra.deployment.customer.personalauto;

import com.socotra.coremodel.DocumentSelectionAction;
import com.socotra.deployment.customer.DocumentSelectionPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DocSelectionPlugin implements DocumentSelectionPlugin {
    private static final Logger logger = LoggerFactory.getLogger(DocumentSelectionPlugin.class);

    @Override
    public Map<String, DocumentSelectionAction> selectDocuments(PersonalAutoQuoteRequest request) {
        var quote = request.quote();
        var result = new java.util.HashMap<String, DocumentSelectionAction>(Map.of(
                "Disclosure", DocumentSelectionAction.generate,
                "Forms", DocumentSelectionAction.generate
        ));

        if (quote.data().convictions().equals("Yes") || quote.data().suspendedLicense().equals("Yes")) {
            result.put("SR22", DocumentSelectionAction.generate);
        }

        logger.info("request={}, result={}", request, result);
        return result;
    }
}

