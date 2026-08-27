import { NavLink } from 'react-router-dom'

function Navigation() {
  return (
    <nav className="navigation" aria-label="Primary navigation">
      <NavLink to="/" end>
        Vending Machine
      </NavLink>
      <NavLink to="/products">Product Management</NavLink>
    </nav>
  )
}

export default Navigation
