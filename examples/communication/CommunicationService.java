package com.epipoli.starter.communication;

import java.util.Map;
import com.epipoli.commons.exceptions.HighwaysException;
import com.epipoli.communication.sender.CommunicationChannelTypeEnum;
import com.epipoli.communication.sender.CommunicationSender;
import com.epipoli.communication.sender.TransactionalCommunication;
import io.micronaut.http.annotation.QueryValue;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.Email;

@Singleton
public class CommunicationService {

    CommunicationSender communicationSender;

    public CommunicationService(CommunicationSender communicationSender){
        this.communicationSender = communicationSender;
    }


    public void sendEmail(@QueryValue @Email String recipient){
        Map<String, String> trxData = Map.of(
        "firstname", "Mario",
        "lastname", "Rossi",
        "customValue", "value"
        );
        TransactionalCommunication transactionalCommunication = new TransactionalCommunication(CommunicationChannelTypeEnum.EMAIL, "DEMO_EMAIL", recipient, trxData); 
        try {
            communicationSender.send(transactionalCommunication);
        } catch (Exception e) {
            e.printStackTrace();
            throw new HighwaysException(e.getMessage()); 
        }
    }


    public void sendSMS(){ 
        Map<String, String> trxData = Map.of(
        "firstname", "Mario",
        "lastname", "Rossi",
        "customValue", "value"
        );
        String recipient = "+391111111111";
        TransactionalCommunication transactionalCommunication = new TransactionalCommunication(CommunicationChannelTypeEnum.SMS, "DEMO_SMS", recipient, trxData); 
        try {
            communicationSender.send(transactionalCommunication);
        } catch (Exception e) {
            e.printStackTrace();
            throw new HighwaysException(e.getMessage()); 
        }
    }
    
}
