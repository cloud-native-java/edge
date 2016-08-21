package edge ;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// <1>
interface ClientDetailsRepository extends JpaRepository<ClientDetails, Long> {

	Optional<ClientDetails> findByClientId(String clientId);
}
