package com.hanghae.mungnayng.repository;

import com.hanghae.mungnayng.domain.zzim.Zzim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ZzimRepository extends JpaRepository<Zzim, Long> {

    Optional<Zzim> findByItemIdAndZzimedBy(Long itemId, String nickname);

    int countAllByItemId(Long itemId);

}
