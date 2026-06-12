package com.smartstocks.product.service.provider;

import com.smartstocks.product.models.EmailProviderType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory that resolves the correct IEmailProvider based on EmailProviderType.
 * To add a new provider: implement IEmailProvider, register it here — no other changes needed.
 */
@Component
public class EmailProviderFactory {

    private final Map<EmailProviderType, IEmailProvider> providers = new EnumMap<>(EmailProviderType.class);

    public EmailProviderFactory(
            SesProvider sesProvider,
            SendGridProvider sendGridProvider,
            MailgunProvider mailgunProvider,
            SmtpProvider smtpProvider) {

        providers.put(EmailProviderType.SES, sesProvider);
        providers.put(EmailProviderType.SENDGRID, sendGridProvider);
        providers.put(EmailProviderType.MAILGUN, mailgunProvider);
        providers.put(EmailProviderType.SMTP, smtpProvider);
    }

    /**
     * Returns the provider for the given type.
     *
     * @throws IllegalArgumentException if no provider is registered for the type
     */
    public IEmailProvider get(EmailProviderType type) {
        IEmailProvider provider = providers.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("No email provider registered for type: " + type);
        }
        return provider;
    }
}
