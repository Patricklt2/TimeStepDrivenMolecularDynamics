import pandas as pd

def load_simulation_data(filename):
    try:
        df = pd.read_csv(filename, delimiter=';')
        return df
    except FileNotFoundError:
        print(f"Error: No se encontró el archivo. {filename}")
        return None