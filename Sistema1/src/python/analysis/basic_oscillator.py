import numpy as np

def calculate_analytical_position(time_array, k, m, gamma, initial_amplitude=1.0):
    sqrt_term = (k / m) - (gamma**2 / (4 * m**2))

    omega_prime = np.sqrt(sqrt_term)
    
    position = initial_amplitude * np.exp(-(gamma / (2 * m)) * time_array) * np.cos(omega_prime * time_array)
    
    return position