-- Elimina el usuario admin creado con hash placeholder en V1.
-- El DataSeeder lo recreará con un hash Argon2id real al arrancar.

DELETE FROM user_roles WHERE user_id = (SELECT id FROM users WHERE username = 'admin');
DELETE FROM users WHERE username = 'admin';
