package cz.jirikfi.monitoringsystembackend.Entities;

import cz.jirikfi.monitoringsystembackend.Services.GenerateUUIDService;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pictures")
public class Picture {

    @Id
    @GeneratedValue
    @Builder.Default
    private UUID id = GenerateUUIDService.v7();

    @Column(unique = true, nullable = false)
    private String filename;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(columnDefinition = "bytea",  nullable = false)
    private byte[] data;

    @Column(name = "content_type", length = 50)
    @Builder.Default
    private String contentType = "image/png";

}
