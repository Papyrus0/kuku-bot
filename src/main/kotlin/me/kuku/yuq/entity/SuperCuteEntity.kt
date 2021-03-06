package me.kuku.yuq.entity

import javax.persistence.*

@Entity
@Table(name = "superCute")
data class SuperCuteEntity(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Int? = null,
        @Column(unique = true)
        var qq: Long = 0L,
        var token: String = ""
)