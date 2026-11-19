package cl.pokestore.backend.contacto;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactoService {

    private final MensajeContactoRepository repository;

    public ContactoService(MensajeContactoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void enviar(ContactoRequest request) {
        repository.save(new MensajeContactoEntity(
                request.nombre().strip(),
                request.email(),
                request.mensaje().strip()
        ));
    }
}
