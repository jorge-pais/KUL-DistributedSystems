package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Content;

import java.io.IOException;
import java.util.List;

public class EmailService {

    private static final String SENDGRID_API_KEY = "OUR_SENDGRID_API_KEY"; // Replace by our  SendGrid API key
    private static final String FROM_EMAIL = "our-email@example.com"; // Replace by our sender email address

    public static void sendBookingConfirmation(String toEmail, boolean isSuccess, List<Ticket> tickets) {
        Email from = new Email(FROM_EMAIL);
        Email to = new Email(toEmail);

        String subject; Content content;
        
        if (isSuccess && tickets != null) {
            content = new Content("text/plain", "Your booking was successful!\n\nTicket Information:\n" + getTicketInfo(tickets));
            subject = "Booking Confirmation";
        } else {
            content = new Content("text/plain", "Booking failed. Please try again.");
            subject = "Booking Failure";
        }

        Mail mail = new Mail(from, subject, to, content);

        sendEmail(mail);
    }

    private static void sendEmail(Mail mail) {
        SendGrid sg = new SendGrid(SENDGRID_API_KEY);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);
            System.out.println(response.getStatusCode());
            System.out.println(response.getBody());
            System.out.println(response.getHeaders());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static String getTicketInfo(List<Ticket> tickets) {
        StringBuilder ticketInfo = new StringBuilder();
        for (Ticket ticket : tickets) {
            ticketInfo.append(ticket.toString()).append("\n");
        }
        return ticketInfo.toString();
    }

}