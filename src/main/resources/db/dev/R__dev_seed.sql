INSERT INTO books (title, isbn, author, published_year, total_copies, available_copies) VALUES
    ('Effective Java', '978-0134685991', 'Joshua Bloch', 2018, 3, 3),
    ('Clean Code', '978-0132350884', 'Robert C. Martin', 2008, 2, 2),
    ('Domain-Driven Design', '978-0321125217', 'Eric Evans', 2003, 1, 1),
    ('Designing Data-Intensive Applications', '978-1449373320', 'Martin Kleppmann', 2017, 2, 2)
ON CONFLICT (isbn) DO NOTHING;

INSERT INTO members (name, email, phone, status) VALUES
    ('Ana Souza', 'ana@example.com', '11999990001', 'ACTIVE'),
    ('Bruno Lima', 'bruno@example.com', '11999990002', 'ACTIVE'),
    ('Carla Dias', 'carla@example.com', NULL, 'SUSPENDED')
ON CONFLICT (email) DO NOTHING;