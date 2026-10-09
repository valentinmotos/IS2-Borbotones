import { useEffect, useState } from 'react'
import './styles.css'

const API_URL = '/api/books'

function App() {
  const [books, setBooks] = useState([])
  const [name, setName] = useState('')
  const [isbnNumber, setIsbnNumber] = useState('')
  const [publishDate, setPublishDate] = useState('')
  const [price, setPrice] = useState('')
  const [bookType, setBookType] = useState('HARDCOVER')
  const [editingId, setEditingId] = useState(null)
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  async function loadBooks() {
    setLoading(true)
    setError('')

    try {
      const response = await fetch(API_URL)
      if (!response.ok) throw new Error('No se pudo conectar con el backend.')

      const data = await response.json()
      setBooks(data)
    } catch {
      setError('No se pudo conectar con el backend. Inicia Spring Boot en localhost:8080.')
    }

    setLoading(false)
  }

  useEffect(() => {
    loadBooks()
  }, [])

  function clearForm() {
    setName('')
    setIsbnNumber('')
    setPublishDate('')
    setPrice('')
    setBookType('HARDCOVER')
    setEditingId(null)
  }

  async function saveBook(event) {
    event.preventDefault()
    setError('')

    const book = {
      name,
      isbnNumber,
      publishDate,
      price: Number(price),
      bookType,
    }

    const url = editingId ? `${API_URL}/${editingId}` : API_URL
    const method = editingId ? 'PUT' : 'POST'

    try {
      const response = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(book),
      })

      if (!response.ok) throw new Error('No se pudo guardar el libro.')

      clearForm()
      loadBooks()
    } catch {
      setError('No se pudo guardar el libro. Revisa los datos e intenta otra vez.')
    }
  }

  function editBook(book) {
    setName(book.name)
    setIsbnNumber(book.isbnNumber)
    setPublishDate(book.publishDate)
    setPrice(book.price)
    setBookType(book.bookType)
    setEditingId(book.id)
  }

  async function deleteBook(id) {
    if (!window.confirm('¿Quieres eliminar este libro?')) return

    try {
      const response = await fetch(`${API_URL}/${id}`, { method: 'DELETE' })
      if (!response.ok) throw new Error('No se pudo eliminar el libro.')

      loadBooks()
    } catch {
      setError('No se pudo eliminar el libro.')
    }
  }

  const filteredBooks = books.filter((book) => {
    const text = search.toLowerCase()
    return book.name.toLowerCase().includes(text) || book.isbnNumber.toLowerCase().includes(text)
  })

  return (
    <main className="container">
      <h1>📚 Catalogue Management System</h1>

      <section className="section">
        <h2>{editingId ? 'Editar libro' : 'Agregar libro'}</h2>

        <form className="book-form" onSubmit={saveBook}>
          <label>
            Nombre del libro
            <input value={name} onChange={(event) => setName(event.target.value)} required />
          </label>

          <label>
            Número ISBN
            <input value={isbnNumber} onChange={(event) => setIsbnNumber(event.target.value)} required />
          </label>

          <label>
            Fecha de publicación
            <input type="date" value={publishDate} onChange={(event) => setPublishDate(event.target.value)} required />
          </label>

          <label>
            Precio
            <input type="number" min="0" step="0.01" value={price} onChange={(event) => setPrice(event.target.value)} required />
          </label>

          <label>
            Formato
            <select value={bookType} onChange={(event) => setBookType(event.target.value)}>
              <option value="HARDCOVER">Tapa dura</option>
              <option value="SOFTCOVER">Tapa blanda</option>
              <option value="EBOOK">Digital</option>
            </select>
          </label>

          <div className="form-buttons">
            <button type="submit">{editingId ? 'Guardar cambios' : 'Agregar libro'}</button>
            {editingId && <button type="button" className="secondary" onClick={clearForm}>Cancelar</button>}
          </div>
        </form>
      </section>

      <section className="section">
        <div className="catalogue-heading">
          <h2>Catálogo de libros</h2>
          <input
            className="search"
            placeholder="Buscar por nombre o ISBN"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>

        {error && <p className="error">{error}</p>}
        {loading && <p>Cargando libros...</p>}

        {!loading && !error && (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Nombre</th>
                  <th>ISBN</th>
                  <th>Fecha de publicación</th>
                  <th>Precio</th>
                  <th>Formato</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {filteredBooks.map((book) => (
                  <tr key={book.id}>
                    <td>{book.id}</td>
                    <td>{book.name}</td>
                    <td>{book.isbnNumber}</td>
                    <td>{book.publishDate}</td>
                    <td>${book.price}</td>
                    <td>{book.bookType}</td>
                    <td className="actions">
                      <button type="button" onClick={() => editBook(book)}>Editar</button>
                      <button type="button" className="danger" onClick={() => deleteBook(book.id)}>Eliminar</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <button type="button" className="secondary reload" onClick={loadBooks}>Recargar lista</button>
      </section>
    </main>
  )
}

export default App
