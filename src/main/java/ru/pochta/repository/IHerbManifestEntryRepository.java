package ru.pochta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.pochta.model.IHerbManifestEntry;

@Repository
public interface IHerbManifestEntryRepository extends JpaRepository<IHerbManifestEntry, Long> {

}
