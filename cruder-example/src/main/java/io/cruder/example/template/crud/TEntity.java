package io.cruder.example.template.crud;

import io.cruder.apt.Template;

@Template
public interface TEntity {
	Wrapper.Id getId();

	public interface Wrapper {
	    public interface Id {
	    }
	}
	
}