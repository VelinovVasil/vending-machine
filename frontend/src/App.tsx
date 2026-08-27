import { Route, Routes } from 'react-router-dom'
import Navigation from './components/layout/Navigation'
import ProductsPage from './pages/ProductsPage'
import VendingPage from './pages/VendingPage'

function App() {
  return (
    <div className="app-shell">
      <header className="site-header">
        <Navigation />
      </header>
      <main className="page-container">
        <Routes>
          <Route path="/" element={<VendingPage />} />
          <Route path="/products" element={<ProductsPage />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
