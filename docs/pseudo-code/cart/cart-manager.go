package cart

type CartAction struct {
	productId string
	quantityAjustment int
}

/*
	CartManager
		- This is a template method object
		- Stateless
		- Distributed
		- Singleton
*/
type CartManager struct {
	CartValidator
	CartEventPublisher
}

func (m *CartManager) UpdateCart(cartId string, action CartAction) error {
	
	// This operation should be done atomically 
	if ok, err := m.SoftValidation(cartId, action); !ok {
		return err
	}
	go m.PushCartSync(cartId, action)

	return nil
}


/*
	Cart Validator
*/
type CartValidator interface {
	SoftValidation(cartId string, action CartAction) (bool, error)
}


/*
	Cart Event
*/
type CartObserver interface {
	PushCartSync(cartId string, action CartAction)
}


/*
	Cart Command Executor
*/