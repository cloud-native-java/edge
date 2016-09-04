package auth;

import auth.clients.Client;
import auth.clients.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
public class DataCommandLineRunner implements CommandLineRunner {

    private final ClientRepository clientRepository;

    @Autowired
    public DataCommandLineRunner(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        Stream.of("acme,acmesecret", "html5,secret", "android,secret", "ios,secret")
            .map(x -> x.split(","))
            .forEach(x -> clientRepository.save(new Client(x[0], x[1])));
    }
}
