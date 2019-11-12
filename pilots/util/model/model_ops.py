
import pickle
import importlib

def get_model( model_name ):
    return importlib.import_module( model_name )

def save_model( model, model_name ):
    model_filename = 'models/' + model_name + '.model'
    m_out_file = open( model_filename, 'wb' )
    s = pickle.dump( model, m_out_file )
    m_out_file.close()

def load_model( model_name ):
    model_filename = 'models/' + model_name + '.model'
    m_in_file = open( model_filename, 'rb' )
    new_model = pickle.load( m_in_file )
    m_in_file.close()
    return new_model

def run_model( model, model_name, data ):
    mm = get_model( model_name )
    return mm.run( model, data )

    