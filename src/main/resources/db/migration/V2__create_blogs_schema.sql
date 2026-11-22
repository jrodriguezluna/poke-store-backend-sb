CREATE TABLE blogs (
    id INT NOT NULL AUTO_INCREMENT,
    etiqueta VARCHAR(100) NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    resumen VARCHAR(1000) NOT NULL,
    autor VARCHAR(100) NOT NULL,
    imagen VARCHAR(500) NULL,
    texto_alternativo VARCHAR(255) NULL,
    introduccion TEXT NOT NULL,
    consejo TEXT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_blogs_creado_en (creado_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE blog_secciones (
    id INT NOT NULL AUTO_INCREMENT,
    blog_id INT NOT NULL,
    orden INT NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    texto TEXT NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_blog_secciones_blog_orden (blog_id, orden),
    CONSTRAINT fk_blog_secciones_blog
        FOREIGN KEY (blog_id) REFERENCES blogs (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
