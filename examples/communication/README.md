# Communication Example

## Scopo
Gestire l’invio di comunicazioni (email e SMS) tramite il servizio centralizzato.

## Architettura
- CommunicationService
  - sendEmail(String recipient)
  - sendSMS()
- CommunicationSender (framework custom)
- TransactionalCommunication (DTO per invio)

## Configurazione
- Le proprietà del canale email/SMS si trovano in `application.yml`
- Personalizza template e valori del DTO tramite trxData
- "DEMO_SMS" e "DEMO_EMAIL" sono i nomi dei template, potrebbero essere inseriti in `application.yml`

## Regole importanti
- Validazione email con `@Email`
- Singleton per il service

Consulta:
- [Exception ](../exceptions/README.md) per creare e gestire eccezioni