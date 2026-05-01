-- Remove fixed NUMERIC precision so monetary columns have no upper bound.
-- PostgreSQL NUMERIC without precision/scale stores values of arbitrary size.

-- characters
ALTER TABLE characters
    ALTER COLUMN cash            TYPE NUMERIC USING cash,
    ALTER COLUMN net_worth       TYPE NUMERIC USING net_worth,
    ALTER COLUMN cumulative_evaded_taxes TYPE NUMERIC USING cumulative_evaded_taxes;

-- jobs
ALTER TABLE jobs
    ALTER COLUMN salary TYPE NUMERIC USING salary;

-- investments
ALTER TABLE investments
    ALTER COLUMN amount_invested TYPE NUMERIC USING amount_invested,
    ALTER COLUMN current_value   TYPE NUMERIC USING current_value,
    ALTER COLUMN quantity        TYPE NUMERIC USING quantity;

-- stocks
ALTER TABLE stocks
    ALTER COLUMN current_price TYPE NUMERIC USING current_price;

-- stock_price_history
ALTER TABLE stock_price_history
    ALTER COLUMN price TYPE NUMERIC USING price;

-- collectibles
ALTER TABLE collectibles
    ALTER COLUMN base_value TYPE NUMERIC USING base_value,
    ALTER COLUMN price      TYPE NUMERIC USING price;

-- monthly_expenses
ALTER TABLE monthly_expenses
    ALTER COLUMN amount TYPE NUMERIC USING amount;

-- event_log
ALTER TABLE event_log
    ALTER COLUMN amount_effect TYPE NUMERIC USING amount_effect;

-- monthly_snapshots
ALTER TABLE monthly_snapshots
    ALTER COLUMN cash           TYPE NUMERIC USING cash,
    ALTER COLUMN net_worth      TYPE NUMERIC USING net_worth,
    ALTER COLUMN total_income   TYPE NUMERIC USING total_income,
    ALTER COLUMN total_expenses TYPE NUMERIC USING total_expenses;

-- real_estate_catalog
ALTER TABLE real_estate_catalog
    ALTER COLUMN purchase_price TYPE NUMERIC USING purchase_price,
    ALTER COLUMN monthly_rent   TYPE NUMERIC USING monthly_rent,
    ALTER COLUMN rent_savings   TYPE NUMERIC USING rent_savings;

-- real_estate (owned)
ALTER TABLE real_estate
    ALTER COLUMN purchase_price TYPE NUMERIC USING purchase_price;

-- player_loans
ALTER TABLE player_loans
    ALTER COLUMN amount_borrowed  TYPE NUMERIC USING amount_borrowed,
    ALTER COLUMN amount_remaining TYPE NUMERIC USING amount_remaining,
    ALTER COLUMN monthly_payment  TYPE NUMERIC USING monthly_payment;

-- gambling_sessions
ALTER TABLE gambling_sessions
    ALTER COLUMN bet_amount    TYPE NUMERIC USING bet_amount,
    ALTER COLUMN payout_amount TYPE NUMERIC USING payout_amount;

-- countries
ALTER TABLE countries
    ALTER COLUMN travel_cost TYPE NUMERIC USING travel_cost;

-- lifestyle_item_catalog
ALTER TABLE lifestyle_item_catalog
    ALTER COLUMN cost         TYPE NUMERIC USING cost,
    ALTER COLUMN monthly_cost TYPE NUMERIC USING monthly_cost;

-- needs_items
ALTER TABLE needs_items
    ALTER COLUMN price TYPE NUMERIC USING price;

-- personal_best_net_worth (already larger, unify with rest)
ALTER TABLE characters
    ALTER COLUMN personal_best_net_worth TYPE NUMERIC USING personal_best_net_worth;
