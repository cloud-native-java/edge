package auth;

import auth.accounts.Account;
import auth.accounts.AccountRepository;
import auth.clients.Client;
import auth.clients.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;


@Component
class DataCommandLineRunner implements CommandLineRunner {

    private final AccountRepository accountRepository;

    private final ClientRepository clientRepository;

    @Autowired
    public DataCommandLineRunner(AccountRepository accountRepository, ClientRepository clientRepository) {
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        Stream.of("dsyer,cloud", "pwebb,boot", "mminella,batch", "rwinch,security", "jlong,spring")
                .map(s -> s.split(","))
                .forEach(tuple -> accountRepository.save(new Account(tuple[0], tuple[1], true)));

        Stream.of("html5,secret", "android,secret")
                .map(x -> x.split(","))
                .forEach(x -> clientRepository.save(new Client(x[0], x[1])));
    }
}
