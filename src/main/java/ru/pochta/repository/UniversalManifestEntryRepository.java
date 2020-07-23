package ru.pochta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.pochta.model.UniversalManifestEntry;

@Repository
public interface UniversalManifestEntryRepository extends JpaRepository<UniversalManifestEntry, Long> {

}
